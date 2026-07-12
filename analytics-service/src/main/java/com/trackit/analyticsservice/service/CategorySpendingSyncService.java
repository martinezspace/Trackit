package com.trackit.analyticsservice.service;

import com.trackit.analyticsservice.client.BankAccountServiceClient;
import com.trackit.analyticsservice.dto.client.bank.BankTransactionDTO;
import com.trackit.analyticsservice.dto.response.CategorySpendingSummaryResponseDTO;
import com.trackit.analyticsservice.exception.ResourceNotFoundException;
import com.trackit.analyticsservice.mapper.CategorySpendingSummaryMapper;
import com.trackit.analyticsservice.model.CategorySpendingSummary;
import com.trackit.analyticsservice.repository.CategorySpendingSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySpendingSyncService {

    private final CategorySpendingSummaryRepository repository;
    private final CategorySpendingSummaryMapper mapper;
    private final BankAccountServiceClient bankAccountClient;

    // --- Sync ---

    public void sync(UUID userId, int year, int month) {
        log.info("Starting category spending sync for user {} period {}/{}", userId, year, month);

        YearMonth period = YearMonth.of(year, month);
        LocalDate from = period.atDay(1);
        LocalDate to = period.atEndOfMonth();

        List<BankTransactionDTO> transactions = bankAccountClient.getTransactions(userId, from, to);

        // Only OUTBOUND transactions with a category contribute to spending summaries
        Map<String, List<BankTransactionDTO>> byCategory = transactions.stream()
                .filter(tx -> "OUTBOUND".equals(tx.getDirection()))
                .filter(tx -> tx.getCategoryId() != null)
                .collect(Collectors.groupingBy(BankTransactionDTO::getCategoryId));

        for (Map.Entry<String, List<BankTransactionDTO>> entry : byCategory.entrySet()) {
            UUID categoryId = UUID.fromString(entry.getKey());
            List<BankTransactionDTO> categoryTxs = entry.getValue();

            BigDecimal total = categoryTxs.stream()
                    .filter(tx -> tx.getAmount() != null)
                    .map(tx -> new BigDecimal(tx.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BankTransactionDTO sample = categoryTxs.get(0);

            Optional<CategorySpendingSummary> existing = repository
                    .findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(userId, categoryId, year, month);

            CategorySpendingSummary summary = existing.orElseGet(CategorySpendingSummary::new);
            summary.setUserId(userId);
            summary.setCategoryId(categoryId);
            summary.setCategoryName(sample.getCategoryName());
            summary.setCategoryColor(sample.getCategoryColor());
            summary.setPeriodYear(year);
            summary.setPeriodMonth(month);
            summary.setTotalAmount(total);
            summary.setTransactionCount(categoryTxs.size());
            summary.setCurrency(sample.getCurrency() != null ? sample.getCurrency() : "EUR");

            repository.save(summary);
        }

        log.info("Category spending sync complete for user {} period {}/{} — {} categories",
                userId, year, month, byCategory.size());
    }

    // --- Queries ---

    public List<CategorySpendingSummaryResponseDTO> getByPeriod(UUID userId, int year, int month) {
        return repository.findByUserIdAndPeriodYearAndPeriodMonthOrderByTotalAmountDesc(userId, year, month)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    public CategorySpendingSummaryResponseDTO getByPeriodAndCategory(UUID userId, int year, int month, UUID categoryId) {
        return repository.findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(userId, categoryId, year, month)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No spending summary found for category " + categoryId + " period " + year + "/" + month));
    }
}