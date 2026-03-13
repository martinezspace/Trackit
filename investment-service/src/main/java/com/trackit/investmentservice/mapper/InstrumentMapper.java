package com.trackit.investmentservice.mapper;

import com.trackit.investmentservice.dto.InstrumentCreateDTO;
import com.trackit.investmentservice.dto.InstrumentResponseDTO;
import com.trackit.investmentservice.model.Instrument;
import org.springframework.stereotype.Component;

@Component
public class InstrumentMapper {

    //Entity => CreateDTO
    public InstrumentResponseDTO toResponseDTO(Instrument instrument) {
        InstrumentResponseDTO response = new InstrumentResponseDTO();
        response.setId(instrument.getId().toString());
        response.setIsin(instrument.getIsin());
        response.setTicker(instrument.getTicker());
        response.setName(instrument.getName());
        response.setInstrumentType(instrument.getInstrumentType().name());
        response.setCurrency(instrument.getCurrency());
        response.setExchange(instrument.getExchange());
        response.setCountry(instrument.getCountry());
        response.setCreatedAt(instrument.getCreatedAt() != null
                ? instrument.getCreatedAt().toString()
                : null);
        return response;
    }
    //CreateDTO => Entity
    public Instrument toEntity(InstrumentCreateDTO request) {
        Instrument instrument = new Instrument();
        instrument.setIsin(request.getIsin());
        instrument.setTicker(request.getTicker());
        instrument.setName(request.getName());
        instrument.setInstrumentType(request.getInstrumentType());
        instrument.setCurrency(request.getCurrency());
        instrument.setExchange(request.getExchange());
        instrument.setCountry(request.getCountry());
        return instrument;
    }
}
