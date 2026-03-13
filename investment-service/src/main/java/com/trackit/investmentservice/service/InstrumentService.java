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

    //Commands
    public InstrumentResponseDTO createInstrument(InstrumentCreateDTO request) {
        //Prevent duplicate ISIN - check before instering
        if (instrumentRepository.existsByIsin(request.getIsin())) {
            throw new IllegalArgumentException("Instrument already exists with ISIN: " + request.getIsin());
        }
        Instrument instrument = instrumentMapper.toEntity(request);
        return instrumentMapper.toResponseDTO(instrumentRepository.save(instrument));
    }

    //Used during CSV import - find existing or create new instrument
    //Avoids duplicate ISINs when the same instrument appears across multiple imports
    public InstrumentResponseDTO findOrCreate(InstrumentCreateDTO request) {
        return instrumentRepository.findByIsin(request.getIsin())
                .map(instrumentMapper::toResponseDTO)
                .orElseGet(() -> createInstrument(request));
    }
}
