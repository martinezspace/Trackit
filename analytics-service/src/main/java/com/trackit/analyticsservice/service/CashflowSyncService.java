package com.trackit.analyticsservice.service;

import com.trackit.analyticsservice.client.BankAccountServiceClient;
import com.trackit.analyticsservice.dto.client.bank.BankTransactionDTO;
import com.trackit.analyticsservice.dto.response.CashflowSummaryResponseDTO;
import com.trackit.analyticsservice.exception.ResourceNotFoundException;
import com.trackit.analyticsservice.mapper.CashflowSummaryMapper;
import com.trackit.analyticsservice.model.CashflowSummary;
import com.trackit.analyticsservice.repository.CashflowSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashflowSyncService {

    private final CashflowSummaryRepository repository;
    private final CashflowSummaryMapper mapper;
    private final BankAccountServiceClient bankAccountClient;

    // --- Sync ---

    public void sync(UUID userId, int year, int month) {
        log.info("Starting cashflow sync for user {} period {}/{}", userId, year, month);

        YearMonth period = YearMonth.of(year, month);
        LocalDate from = period.atDay(1);
        LocalDate to = period.atEndOfMonth();

        List<BankTransactionDTO> transactions = bankAccountClient.getTransactions(userId, from, to);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        String currency = "EUR";

        for (BankTransactionDTO tx : transactions) {
            if (tx.getAmount() == null) continue;
            BigDecimal amount = new BigDecimal(tx.getAmount());
            if (tx.getCurrency() != null) currency = tx.getCurrency();

            if ("INBOUND".equals(tx.getDirection())) {
                totalIncome = totalIncome.add(amount);
            } else if ("OUTBOUND".equals(tx.getDirection())) {
                totalExpenses = totalExpenses.add(amount);
            }
        }

        Optional<CashflowSummary> existing = repository.findByUserIdAndPeriodYearAndPeriodMonth(userId, year, month);
        CashflowSummary summary = existing.orElseGet(CashflowSummary::new);
        summary.setUserId(userId);
        summary.setPeriodYear(year);
        summary.setPeriodMonth(month);
        summary.setTotalIncome(totalIncome);
        summary.setTotalExpenses(totalExpenses);
        summary.setNetCashflow(totalIncome.subtract(totalExpenses));
        summary.setTransactionCount(transactions.size());
        summary.setCurrency(currency);

        repository.save(summary);
        log.info("Cashflow sync complete for user {} period {}/{} — {} transactions", userId, year, month, transactions.size());
    }

    // --- Queries ---

    public List<CashflowSummaryResponseDTO> getAll(UUID userId) {
        return repository.findByUserIdOrderByPeriodYearDescPeriodMonthDesc(userId)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    public CashflowSummaryResponseDTO getByPeriod(UUID userId, int year, int month) {
        return repository.findByUserIdAndPeriodYearAndPeriodMonth(userId, year, month)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No cashflow summary found for user " + userId + " period " + year + "/" + month));
    }
}