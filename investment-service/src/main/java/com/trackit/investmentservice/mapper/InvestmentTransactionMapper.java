package com.trackit.investmentservice.mapper;

import com.trackit.investmentservice.dto.InvestmentTransactionResponseDTO;
import com.trackit.investmentservice.model.InvestmentTransaction;
import org.springframework.stereotype.Component;

@Component
public class InvestmentTransactionMapper {

    //Entity => ResponseDTO
    public InvestmentTransactionResponseDTO toResponseDTO(InvestmentTransaction transaction) {
        InvestmentTransactionResponseDTO response = new InvestmentTransactionResponseDTO();

        response.setId(transaction.getId().toString());
        response.setAccountId(transaction.getAccount().getId().toString());

        //Navigate ManyToOne relationship to get instrument details
        response.setInstrumentId(transaction.getInstrument().getId().toString());
        response.setInstrumentName(transaction.getInstrument().getName());
        response.setInstrumentTicker(transaction.getInstrument().getTicker());
        response.setIsin(transaction.getInstrument().getIsin());

        response.setExternalId(transaction.getExternalId());
        response.setTransactionType(transaction.getTransactionType().name());

        //BigDecimal => String to avoid floating points issues in JSON
        response.setQuantity(transaction.getQuantity().toPlainString());
        response.setPrice(transaction.getPrice().toPlainString());
        response.setAmount(transaction.getAmount().toPlainString());

        response.setCurrency(transaction.getCurrency());
        response.setTransactionDate(transaction.getTransactionDate().toString());
        response.setCancelled(transaction.isCancelled());
        response.setCreatedAt(transaction.getCreatedAt() != null
                ? transaction.getCreatedAt().toString()
                : null);

        return response;
    }
}
