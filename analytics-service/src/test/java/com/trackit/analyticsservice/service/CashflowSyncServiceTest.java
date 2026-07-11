package com.trackit.analyticsservice.service;

import com.trackit.analyticsservice.client.BankAccountServiceClient;
import com.trackit.analyticsservice.dto.client.bank.BankTransactionDTO;
import com.trackit.analyticsservice.dto.response.CashflowSummaryResponseDTO;
import com.trackit.analyticsservice.exception.ResourceNotFoundException;
import com.trackit.analyticsservice.mapper.CashflowSummaryMapper;
import com.trackit.analyticsservice.model.CashflowSummary;
import com.trackit.analyticsservice.repository.CashflowSummaryRepository;
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

class CashflowSyncServiceTest {

    @Mock private CashflowSummaryRepository repository;
    @Mock private CashflowSummaryMapper mapper;
    @Mock private BankAccountServiceClient bankAccountClient;

    @InjectMocks
    private CashflowSyncService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
    }

    // sync

    @Test
    void sync_aggregatesIncomeAndExpensesCorrectly() {
        when(bankAccountClient.getTransactions(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        buildTransaction("1000.00", "INBOUND", "EUR"),
                        buildTransaction("500.00", "INBOUND", "EUR"),
                        buildTransaction("300.00", "OUTBOUND", "EUR")
                ));
        when(repository.findByUserIdAndPeriodYearAndPeriodMonth(userId, 2024, 3))
                .thenReturn(Optional.empty());

        service.sync(userId, 2024, 3);

        ArgumentCaptor<CashflowSummary> captor = ArgumentCaptor.forClass(CashflowSummary.class);
        verify(repository).save(captor.capture());
        CashflowSummary saved = captor.getValue();

        assertThat(saved.getTotalIncome()).isEqualByComparingTo("1500.00");
        assertThat(saved.getTotalExpenses()).isEqualByComparingTo("300.00");
        assertThat(saved.getNetCashflow()).isEqualByComparingTo("1200.00");
        assertThat(saved.getTransactionCount()).isEqualTo(3);
        assertThat(saved.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void sync_updatesExistingSummary_whenPeriodAlreadySynced() {
        CashflowSummary existing = new CashflowSummary();
        existing.setUserId(userId);
        existing.setPeriodYear(2024);
        existing.setPeriodMonth(3);

        when(bankAccountClient.getTransactions(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(buildTransaction("2000.00", "INBOUND", "EUR")));
        when(repository.findByUserIdAndPeriodYearAndPeriodMonth(userId, 2024, 3))
                .thenReturn(Optional.of(existing));

        service.sync(userId, 2024, 3);

        verify(repository).save(existing);
        assertThat(existing.getTotalIncome()).isEqualByComparingTo("2000.00");
    }

    @Test
    void sync_handlesEmptyTransactionList() {
        when(bankAccountClient.getTransactions(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(repository.findByUserIdAndPeriodYearAndPeriodMonth(userId, 2024, 3))
                .thenReturn(Optional.empty());

        service.sync(userId, 2024, 3);

        ArgumentCaptor<CashflowSummary> captor = ArgumentCaptor.forClass(CashflowSummary.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTotalIncome()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getTotalExpenses()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getNetCashflow()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getTransactionCount()).isEqualTo(0);
    }

    @Test
    void sync_skipsTransactionsWithNullAmount() {
        BankTransactionDTO nullAmount = buildTransaction(null, "INBOUND", "EUR");

        when(bankAccountClient.getTransactions(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(nullAmount, buildTransaction("500.00", "INBOUND", "EUR")));
        when(repository.findByUserIdAndPeriodYearAndPeriodMonth(userId, 2024, 3))
                .thenReturn(Optional.empty());

        service.sync(userId, 2024, 3);

        ArgumentCaptor<CashflowSummary> captor = ArgumentCaptor.forClass(CashflowSummary.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTotalIncome()).isEqualByComparingTo("500.00");
    }

    // getAll

    @Test
    void getAll_returnsMappedList() {
        CashflowSummary summary = new CashflowSummary();
        CashflowSummaryResponseDTO dto = new CashflowSummaryResponseDTO();

        when(repository.findByUserIdOrderByPeriodYearDescPeriodMonthDesc(userId))
                .thenReturn(List.of(summary));
        when(mapper.toResponseDTO(summary)).thenReturn(dto);

        assertThat(service.getAll(userId)).containsExactly(dto);
    }

    // getByPeriod

    @Test
    void getByPeriod_returnsMappedDTO() {
        CashflowSummary summary = new CashflowSummary();
        CashflowSummaryResponseDTO dto = new CashflowSummaryResponseDTO();

        when(repository.findByUserIdAndPeriodYearAndPeriodMonth(userId, 2024, 3))
                .thenReturn(Optional.of(summary));
        when(mapper.toResponseDTO(summary)).thenReturn(dto);

        assertThat(service.getByPeriod(userId, 2024, 3)).isEqualTo(dto);
    }

    @Test
    void getByPeriod_throwsException_whenPeriodNotFound() {
        when(repository.findByUserIdAndPeriodYearAndPeriodMonth(userId, 2024, 3))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByPeriod(userId, 2024, 3))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No cashflow summary found");
    }

    private BankTransactionDTO buildTransaction(String amount, String direction, String currency) {
        BankTransactionDTO tx = new BankTransactionDTO();
        tx.setAmount(amount);
        tx.setDirection(direction);
        tx.setCurrency(currency);
        return tx;
    }
}