package com.trackit.analyticsservice.service;

import com.trackit.analyticsservice.client.BankAccountServiceClient;
import com.trackit.analyticsservice.dto.client.bank.BankTransactionDTO;
import com.trackit.analyticsservice.dto.response.CategorySpendingSummaryResponseDTO;
import com.trackit.analyticsservice.exception.ResourceNotFoundException;
import com.trackit.analyticsservice.mapper.CategorySpendingSummaryMapper;
import com.trackit.analyticsservice.model.CategorySpendingSummary;
import com.trackit.analyticsservice.repository.CategorySpendingSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CategorySpendingSyncServiceTest {

    @Mock private CategorySpendingSummaryRepository repository;
    @Mock private CategorySpendingSummaryMapper mapper;
    @Mock private BankAccountServiceClient bankAccountClient;

    @InjectMocks
    private CategorySpendingSyncService service;

    private UUID userId;
    private UUID groceriesId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        groceriesId = UUID.randomUUID();
    }

    // sync

    @Test
    void sync_savesOneSummaryPerCategory() {
        UUID transportId = UUID.randomUUID();

        when(bankAccountClient.getTransactions(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        buildTransaction("100.00", "OUTBOUND", groceriesId, "Groceries", "#00FF00"),
                        buildTransaction("200.00", "OUTBOUND", groceriesId, "Groceries", "#00FF00"),
                        buildTransaction("50.00",  "OUTBOUND", transportId, "Transport", "#0000FF")
                ));
        when(repository.findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(any(), any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        service.sync(userId, 2024, 3);

        // Two distinct categories → two saves
        verify(repository, times(2)).save(any(CategorySpendingSummary.class));
    }

    @Test
    void sync_aggregatesTotalCorrectlyPerCategory() {
        when(bankAccountClient.getTransactions(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        buildTransaction("100.00", "OUTBOUND", groceriesId, "Groceries", "#00FF00"),
                        buildTransaction("200.00", "OUTBOUND", groceriesId, "Groceries", "#00FF00")
                ));
        when(repository.findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(
                eq(userId), eq(groceriesId), eq(2024), eq(3)))
                .thenReturn(Optional.empty());

        service.sync(userId, 2024, 3);

        ArgumentCaptor<CategorySpendingSummary> captor = ArgumentCaptor.forClass(CategorySpendingSummary.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(captor.getValue().getTransactionCount()).isEqualTo(2);
        assertThat(captor.getValue().getCategoryName()).isEqualTo("Groceries");
    }

    @Test
    void sync_skipsInboundTransactions() {
        when(bankAccountClient.getTransactions(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        buildTransaction("500.00", "INBOUND", groceriesId, "Groceries", "#00FF00")
                ));

        service.sync(userId, 2024, 3);

        verify(repository, never()).save(any());
    }

    @Test
    void sync_skipsUncategorizedTransactions() {
        BankTransactionDTO uncategorized = buildTransaction("100.00", "OUTBOUND", null, null, null);

        when(bankAccountClient.getTransactions(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(uncategorized));

        service.sync(userId, 2024, 3);

        verify(repository, never()).save(any());
    }

    @Test
    void sync_updatesExistingSummary_whenAlreadySynced() {
        CategorySpendingSummary existing = new CategorySpendingSummary();
        existing.setCategoryId(groceriesId);

        when(bankAccountClient.getTransactions(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(buildTransaction("150.00", "OUTBOUND", groceriesId, "Groceries", "#00FF00")));
        when(repository.findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(
                eq(userId), eq(groceriesId), eq(2024), eq(3)))
                .thenReturn(Optional.of(existing));

        service.sync(userId, 2024, 3);

        verify(repository).save(existing);
        assertThat(existing.getTotalAmount()).isEqualByComparingTo("150.00");
    }

    // getByPeriod

    @Test
    void getByPeriod_returnsMappedList() {
        CategorySpendingSummary summary = new CategorySpendingSummary();
        CategorySpendingSummaryResponseDTO dto = new CategorySpendingSummaryResponseDTO();

        when(repository.findByUserIdAndPeriodYearAndPeriodMonthOrderByTotalAmountDesc(userId, 2024, 3))
                .thenReturn(List.of(summary));
        when(mapper.toResponseDTO(summary)).thenReturn(dto);

        assertThat(service.getByPeriod(userId, 2024, 3)).containsExactly(dto);
    }

    // getByPeriodAndCategory

    @Test
    void getByPeriodAndCategory_returnsMappedDTO() {
        CategorySpendingSummary summary = new CategorySpendingSummary();
        CategorySpendingSummaryResponseDTO dto = new CategorySpendingSummaryResponseDTO();

        when(repository.findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(userId, groceriesId, 2024, 3))
                .thenReturn(Optional.of(summary));
        when(mapper.toResponseDTO(summary)).thenReturn(dto);

        assertThat(service.getByPeriodAndCategory(userId, 2024, 3, groceriesId)).isEqualTo(dto);
    }

    @Test
    void getByPeriodAndCategory_throwsException_whenNotFound() {
        when(repository.findByUserIdAndCategoryIdAndPeriodYearAndPeriodMonth(userId, groceriesId, 2024, 3))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByPeriodAndCategory(userId, 2024, 3, groceriesId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No spending summary found");
    }

    private BankTransactionDTO buildTransaction(String amount, String direction,
                                                UUID categoryId, String categoryName, String categoryColor) {
        BankTransactionDTO tx = new BankTransactionDTO();
        tx.setAmount(amount);
        tx.setDirection(direction);
        tx.setCurrency("EUR");
        tx.setCategoryId(categoryId != null ? categoryId.toString() : null);
        tx.setCategoryName(categoryName);
        tx.setCategoryColor(categoryColor);
        return tx;
    }
}