package com.trackit.analyticsservice.service;

import com.trackit.analyticsservice.client.InvestmentServiceClient;
import com.trackit.analyticsservice.dto.client.investment.InvestmentAccountDTO;
import com.trackit.analyticsservice.dto.client.investment.InvestmentTransactionDTO;
import com.trackit.analyticsservice.dto.response.MonthlyInvestmentSummaryResponseDTO;
import com.trackit.analyticsservice.exception.ResourceNotFoundException;
import com.trackit.analyticsservice.mapper.MonthlyInvestmentSummaryMapper;
import com.trackit.analyticsservice.model.MonthlyInvestmentSummary;
import com.trackit.analyticsservice.repository.MonthlyInvestmentSummaryRepository;
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
public class MonthlyInvestmentSyncService {

    private final MonthlyInvestmentSummaryRepository repository;
    private final MonthlyInvestmentSummaryMapper mapper;
    private final InvestmentServiceClient investmentClient;

    // --- Sync ---

    public void sync(UUID userId, int year, int month) {
        log.info("Starting monthly investment sync for user {} period {}/{}", userId, year, month);

        List<InvestmentAccountDTO> accounts = investmentClient.getInvestmentAccounts(userId);

        for (InvestmentAccountDTO account : accounts) {
            if (!account.isActive()) continue;

            UUID accountId = UUID.fromString(account.getId());
            List<InvestmentTransactionDTO> transactions = investmentClient
                    .getInvestmentTransactions(accountId, userId);

            // Filter to the target month and exclude cancelled transactions
            YearMonth period = YearMonth.of(year, month);
            List<InvestmentTransactionDTO> periodTxs = transactions.stream()
                    .filter(tx -> !tx.isCancelled())
                    .filter(tx -> tx.getTransactionDate() != null)
                    .filter(tx -> {
                        LocalDate date = LocalDate.parse(tx.getTransactionDate());
                        return YearMonth.from(date).equals(period);
                    })
                    .toList();

            BigDecimal contributions = BigDecimal.ZERO;
            BigDecimal withdrawals = BigDecimal.ZERO;
            BigDecimal dividends = BigDecimal.ZERO;

            for (InvestmentTransactionDTO tx : periodTxs) {
                if (tx.getAmount() == null) continue;
                BigDecimal amount = new BigDecimal(tx.getAmount());

                switch (tx.getTransactionType()) {
                    case "BUY"      -> contributions = contributions.add(amount);
                    case "SELL"     -> withdrawals = withdrawals.add(amount);
                    case "DIVIDEND" -> dividends = dividends.add(amount);
                }
            }

            Optional<MonthlyInvestmentSummary> existing = repository
                    .findByUserIdAndAccountIdAndPeriodYearAndPeriodMonth(userId, accountId, year, month);

            MonthlyInvestmentSummary summary = existing.orElseGet(MonthlyInvestmentSummary::new);
            summary.setUserId(userId);
            summary.setAccountId(accountId);
            summary.setAccountType(account.getAccountType());
            summary.setPeriodYear(year);
            summary.setPeriodMonth(month);
            summary.setContributionsTotal(contributions);
            summary.setWithdrawalsTotal(withdrawals);
            summary.setDividendsTotal(dividends);
            // FEE and TAX not yet in TransactionType — stored as zero until enum is extended
            summary.setFeesTotal(BigDecimal.ZERO);
            summary.setTaxesTotal(BigDecimal.ZERO);
            summary.setCurrency(account.getCurrency() != null ? account.getCurrency() : "EUR");

            repository.save(summary);
        }

        log.info("Monthly investment sync complete for user {} period {}/{}", userId, year, month);
    }

    // --- Queries ---

    public List<MonthlyInvestmentSummaryResponseDTO> getAll(UUID userId) {
        return repository.findByUserIdOrderByPeriodYearDescPeriodMonthDesc(userId)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    public List<MonthlyInvestmentSummaryResponseDTO> getByAccount(UUID userId, UUID accountId) {
        return repository.findByUserIdAndAccountIdOrderByPeriodYearDescPeriodMonthDesc(userId, accountId)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    public MonthlyInvestmentSummaryResponseDTO getByAccountAndPeriod(UUID userId, UUID accountId, int year, int month) {
        return repository.findByUserIdAndAccountIdAndPeriodYearAndPeriodMonth(userId, accountId, year, month)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No monthly investment summary found for account " + accountId + " period " + year + "/" + month));
    }
}