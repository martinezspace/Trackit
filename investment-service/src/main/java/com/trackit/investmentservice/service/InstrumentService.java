package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.InstrumentCreateDTO;
import com.trackit.investmentservice.dto.InstrumentResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.InstrumentMapper;
import com.trackit.investmentservice.model.Instrument;
import com.trackit.investmentservice.model.InstrumentType;
import com.trackit.investmentservice.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentMapper instrumentMapper;

    //Queries
    public InstrumentResponseDTO getById(UUID id) {
        Instrument instrument = instrumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument Not Found: " + id));
        return instrumentMapper.toResponseDTO(instrument);
    }

    public InstrumentResponseDTO getByIsin(String isin) {
        Instrument instrument = instrumentRepository.findByIsin(isin)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument Not Found for ISIN: " + isin));
        return instrumentMapper.toResponseDTO(instrument);
    }

    public List<InstrumentResponseDTO> getByType(InstrumentType type) {
        return instrumentRepository.findByInstrumentType(type)
                .stream()
                .map(instrumentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    //Returns InstrumentResponseDTO list, contains both id and ticker
    //PriceWorker uses: id (to save price) + ticker (to call)
    public List<InstrumentResponseDTO> getActiveHoldingInstruments() {
        return instrumentRepository.findInstrumentsWithActiveHoldings()
                .stream()
                .map(instrumentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
    //Commands
    public InstrumentResponseDTO createInstrument(InstrumentCreateDTO request) {
        //If ISIN provided - check for duplicates
        if (request.getIsin() != null && !request.getIsin().isEmpty()) {
            if (instrumentRepository.existsByIsin(request.getIsin())) {
                throw new IllegalArgumentException(
                        "Instrument already exists with ISIN: " + request.getIsin());
            }
        }
        //If no ISIN - check for duplicate name (PPK funds)
        if (request.getIsin() == null || request.getIsin().isEmpty()) {
            if (instrumentRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException(
                        "Instrument already exists with name: " + request.getName());
            }
        }

        Instrument instrument = instrumentMapper.toEntity(request);
        return instrumentMapper.toResponseDTO(instrumentRepository.save(instrument));
    }

    //Used during CSV import - find existing or create new instrument
    public InstrumentResponseDTO findOrCreate(InstrumentCreateDTO request) {
        //Try ISIN first
        if (request.getIsin() != null && !request.getIsin().isEmpty()) {
            return instrumentRepository.findByIsin(request.getIsin())
                    .map(instrumentMapper::toResponseDTO)
                    .orElseGet(() -> createInstrument(request));
        }

        //Fallback to name lookup for PPK funds
        return instrumentRepository.findByName(request.getName())
                .map(instrumentMapper::toResponseDTO)
                .orElseGet(() -> createInstrument(request));
    }

    //Called by PriceWorker after fetching
    // if ISIN is returned, and we don't have it yet, we enrich the instrument record
    public void enrichIsin(String ticker, String isin) {
        instrumentRepository.findByTicker(ticker).ifPresent(instrument -> {
            if (instrument.getIsin() == null && isin != null) {
                instrument.setIsin(isin);
                instrumentRepository.save(instrument);
            }
        });
    }
}
