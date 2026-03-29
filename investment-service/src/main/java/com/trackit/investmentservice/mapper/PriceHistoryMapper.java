package com.trackit.investmentservice.mapper;

import com.trackit.investmentservice.dto.PriceHistoryCreateDTO;
import com.trackit.investmentservice.dto.PriceHistoryResponseDTO;
import com.trackit.investmentservice.model.Instrument;
import com.trackit.investmentservice.model.PriceHistory;
import org.springframework.stereotype.Component;

@Component
public class PriceHistoryMapper {

    //CreateDTO + Instrument => Entity
    //Instrument is passed separately - PriceWorker has instrumentId in the DTO
    //but the entity needs the full Instrument object for the ManyToOne relationship
    public PriceHistory toEntity(PriceHistoryCreateDTO request, Instrument instrument) {
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setInstrument(instrument);
        priceHistory.setPriceDate(request.getPriceDate());
        priceHistory.setClosePrice(request.getClosePrice());
        priceHistory.setCurrency(request.getCurrency());
        priceHistory.setSource(request.getSource());
        return priceHistory;
    }

    //Entity => ResponseDTO
    public PriceHistoryResponseDTO toResponseDTO(PriceHistory priceHistory) {
        PriceHistoryResponseDTO response = new PriceHistoryResponseDTO();
        response.setId(priceHistory.getId().toString());
        response.setInstrumentId(priceHistory.getInstrument().getId().toString());
        response.setInstrumentTicker(priceHistory.getInstrument().getTicker());
        response.setInstrumentName(priceHistory.getInstrument().getName());
        response.setPriceDate(priceHistory.getPriceDate().toString());
        response.setClosePrice(priceHistory.getClosePrice().toPlainString());
        response.setCurrency(priceHistory.getCurrency());
        response.setSource(priceHistory.getSource());
        response.setCreatedAt(priceHistory.getCreatedAt() != null
                ? priceHistory.getCreatedAt().toString()
                : null);
        return response;
    }
}
