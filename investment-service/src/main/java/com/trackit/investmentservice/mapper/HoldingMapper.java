package com.trackit.investmentservice.mapper;

import com.trackit.investmentservice.dto.HoldingResponseDTO;
import com.trackit.investmentservice.model.Holding;
import org.springframework.stereotype.Component;

@Component
public class HoldingMapper {

    public HoldingResponseDTO toResponseDTO(Holding holding) {
        HoldingResponseDTO response = new HoldingResponseDTO();

        response.setId(holding.getId().toString());
        response.setAccountId(holding.getAccount().getId().toString());

        //Navigate ManyToOne to get instrument details
        response.setInstrumentId(holding.getInstrument().getId().toString());
        response.setInstrumentName(holding.getInstrument().getName());
        response.setInstrumentTicker(holding.getInstrument().getTicker());
        response.setIsin(holding.getInstrument().getIsin());

        response.setQuantity(holding.getQuantity().toPlainString());
        response.setAvgPurchasePrice(holding.getAvgPurchasePrice().toPlainString());
        response.setTotalInvested(holding.getTotalInvested().toPlainString());

        //Nullable - only set after PriceWorker runs
        response.setCurrentPrice(holding.getCurrentPrice() != null
                ? holding.getCurrentPrice().toPlainString()
                : null);
        response.setCurrentValue(holding.getCurrentValue() != null
                ? holding.getCurrentValue().toPlainString()
                : null);
        response.setUnrealizedPnL(holding.getUnrealizedPnL() != null
                ? holding.getUnrealizedPnL().toPlainString()
                : null);
        response.setUnrealizedPnlPct(holding.getUnrealizedPnLPct() != null
                ? holding.getUnrealizedPnLPct().toPlainString()
                : null);

        response.setCurrency(holding.getCurrency());

        response.setLastPriceUpdate(holding.getLastPriceUpdate() != null
                ? holding.getLastPriceUpdate().toString()
                : null);
        response.setUpdatedAt(holding.getUpdatedAt() != null
                ? holding.getUpdatedAt().toString()
                : null);

        return response;
    }
}
