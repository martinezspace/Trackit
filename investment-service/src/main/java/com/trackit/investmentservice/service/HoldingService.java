package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.HoldingResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.HoldingMapper;
import com.trackit.investmentservice.model.*;
import com.trackit.investmentservice.repository.HoldingRepository;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import com.trackit.investmentservice.repository.InvestmentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HoldingService {

    private final HoldingRepository holdingRepository;
    private final InvestmentAccountRepository investmentAccountRepository;
    private final InvestmentTransactionRepository transactionRepository;
    private final HoldingMapper holdingMapper;

    //Queries
    public List<HoldingResponseDTO> getAllHoldingsForAccount(UUID accountId, UUID userId) {
        verifyAccountOwnership(accountId, userId);
        return holdingRepository.findByAccount_Id(accountId)
                .stream()
                .map(holdingMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    //Get holding for a specific instrument in an account
    public HoldingResponseDTO getHoldingById(UUID accountId, UUID instrumentId, UUID userId) {
        verifyAccountOwnership(accountId, userId);
        Holding holding = holdingRepository.findByAccount_IdAndInstrument_Id(accountId, instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Holding Not Found For Instrument: " + instrumentId));
        return holdingMapper.toResponseDTO(holding);
    }

    //Recalculation

    @Transactional
    public void recalculateHoldings(UUID accountId) {
        InvestmentAccount account = investmentAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account Not Found: " + accountId));

        List<InvestmentTransaction> transactions = transactionRepository
                .findByAccount_IdAndCancelledFalse(accountId);

        Map<Instrument, List<InvestmentTransaction>> byInstrument = transactions.stream()
                .collect(Collectors.groupingBy(InvestmentTransaction::getInstrument));

        byInstrument.forEach((instrument, txList) ->
                recalculateHoldingForInstrument(account, instrument, txList));

        //Zero out holdings for instruments no longer held
        holdingRepository.findByAccount_Id(accountId).forEach(holding -> {
            boolean stillHeld = byInstrument.containsKey(holding.getInstrument());
            if (!stillHeld) {
                holding.setQuantity(BigDecimal.ZERO);
                holding.setTotalInvested(BigDecimal.ZERO);
                holding.setAvgPurchasePrice(BigDecimal.ZERO);
                holdingRepository.save(holding);
            }
        });
    }
    //Helpers

    private void recalculateHoldingForInstrument(
            InvestmentAccount account,
            Instrument instrument,
            List<InvestmentTransaction> transactions) {

        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;

        for (InvestmentTransaction tx: transactions) {
            if(tx.getTransactionType() == TransactionType.BUY) {
                quantity = quantity.add(tx.getQuantity());
                totalInvested = totalInvested.add(tx.getAmount());
            } else if (tx.getTransactionType() == TransactionType.SELL) {
                quantity = quantity.subtract(tx.getQuantity());
                if (quantity.compareTo(BigDecimal.ZERO) >= 0 && totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal sellRatio = tx.getQuantity()
                            .divide(quantity.add(tx.getQuantity()), 10, RoundingMode.HALF_UP);
                    totalInvested = totalInvested.subtract(totalInvested.multiply(sellRatio));
                }
            }
            //DIVIDEND does not affect quantity or cost basis
        }

        BigDecimal avgPurchasePrice = quantity.compareTo(BigDecimal.ZERO) > 0
                ? totalInvested.divide(quantity, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Holding holding = holdingRepository
                .findByAccount_IdAndInstrument_Id(account.getId(), instrument.getId())
                .orElseGet(() -> {
                    Holding newHolding = new Holding();
                    newHolding.setAccount(account);
                    newHolding.setInstrument(instrument);
                    newHolding.setCurrency(account.getCurrency());
                    return newHolding;
                });

        holding.setQuantity(quantity);
        holding.setTotalInvested(totalInvested.setScale(2, RoundingMode.HALF_UP));
        holding.setAvgPurchasePrice(avgPurchasePrice);

        if (holding.getCurrentPrice() != null) {
            BigDecimal currentValue = quantity.multiply(holding.getCurrentPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            holding.setCurrentValue(currentValue);

            BigDecimal unrealizedPnL = currentValue.subtract(totalInvested)
                    .setScale(2, RoundingMode.HALF_UP);
            holding.setUnrealizedPnL(unrealizedPnL);

            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unrealizedPnLPct = unrealizedPnL
                        .divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                holding.setUnrealizedPnLPct(unrealizedPnLPct);
            }
        }
        holdingRepository.save(holding);
    }

    private void verifyAccountOwnership(UUID accountId, UUID userId) {
        investmentAccountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account Not Found: " + accountId));
    }
}
