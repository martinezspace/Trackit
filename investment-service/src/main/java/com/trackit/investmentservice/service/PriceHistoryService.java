package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.PriceHistoryCreateDTO;
import com.trackit.investmentservice.dto.PriceHistoryResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.PriceHistoryMapper;
import com.trackit.investmentservice.model.Holding;
import com.trackit.investmentservice.model.Instrument;
import com.trackit.investmentservice.model.PriceHistory;
import com.trackit.investmentservice.repository.HoldingRepository;
import com.trackit.investmentservice.repository.InstrumentRepository;
import com.trackit.investmentservice.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final InstrumentRepository instrumentRepository;
    private final HoldingRepository holdingRepository;
    private final PriceHistoryMapper priceHistoryMapper;

    //Queries
    //Latest price for an instrument - used by frontend for current price display
    public PriceHistoryResponseDTO getLatestPrice(UUID instrumentId) {
        return priceHistoryRepository.findTopInstrument_IdOrderByPriceDateDesc(instrumentId)
                .map(priceHistoryMapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("No Price History Found For Instrument: " + instrumentId));
    }

    //Price history for a date range - used for portfolio charts
    public List<PriceHistoryResponseDTO> getPriceHistory(UUID instrumentId, LocalDate from, LocalDate to) {
        return priceHistoryRepository
                .findByInstrument_IdAndPriceDateBetweenOrderByPriceDateAsc(instrumentId, from, to)
                .stream()
                .map(priceHistoryMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    //Commands
    //Save a new price - called by PriceWorker after fetching from Alpha Vantage
    //Also updates all holdings for this instrument with the new price
    @Transactional
    public PriceHistoryResponseDTO savePrice(PriceHistoryCreateDTO request) {
        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Instrument Not Found: " + request.getInstrumentId()));

        //Skip if price already exists for this date
        if (priceHistoryRepository.existsByInstrument_IdAndPriceDate(
                request.getInstrumentId(), request.getPriceDate())) {
            return priceHistoryRepository
                    .findTopInstrument_IdOrderByPriceDateDesc(request.getInstrumentId())
                    .map(priceHistoryMapper::toResponseDTO)
                    .orElseThrow();
        }

        //Save new price
        PriceHistory saved = priceHistoryRepository.save(priceHistoryMapper.toEntity(request, instrument));

        //Update all holdings for this instrument with the new price
        updateHoldingsWithNewPrice(instrument, saved.getClosePrice());

        return priceHistoryMapper.toResponseDTO(saved);
    }

    //Helper
    private void updateHoldingsWithNewPrice(Instrument instrument, BigDecimal newPrice) {
        List<Holding> holdings = holdingRepository.findByInstrument_Id(instrument.getId());

        holdings.forEach(holding -> {
            holding.setCurrentPrice(newPrice);

            BigDecimal currentValue = holding.getQuantity()
                    .multiply(newPrice)
                    .setScale(2, RoundingMode.HALF_UP);
            holding.setCurrentValue(currentValue);

            BigDecimal unrealizedPnL = currentValue
                    .subtract(holding.getTotalInvested())
                    .setScale(2, RoundingMode.HALF_UP);
            holding.setUnrealizedPnL(unrealizedPnL);

            if (holding.getTotalInvested().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unrealizedPnLPct = unrealizedPnL
                        .divide(holding.getTotalInvested(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                holding.setUnrealizedPnLPct(unrealizedPnLPct);
            }

            holding.setLastPriceUpdate(LocalDateTime.now());
        });

        holdingRepository.saveAll(holdings);
    }
}
