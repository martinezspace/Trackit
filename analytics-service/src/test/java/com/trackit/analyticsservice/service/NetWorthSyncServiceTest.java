package com.trackit.analyticsservice.service;

import com.trackit.analyticsservice.client.BankAccountServiceClient;
import com.trackit.analyticsservice.client.InvestmentServiceClient;
import com.trackit.analyticsservice.dto.client.bank.BankAccountDTO;
import com.trackit.analyticsservice.dto.client.investment.InvestmentAccountDTO;
import com.trackit.analyticsservice.dto.client.investment.PortfolioSnapshotDTO;
import com.trackit.analyticsservice.dto.response.NetWorthSnapshotResponseDTO;
import com.trackit.analyticsservice.mapper.NetWorthSnapshotMapper;
import com.trackit.analyticsservice.model.NetWorthSnapshot;
import com.trackit.analyticsservice.repository.NetWorthSnapshotRepository;
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
import static org.mockito.Mockito.*;

class NetWorthSyncServiceTest {

    @Mock private NetWorthSnapshotRepository repository;
    @Mock private NetWorthSnapshotMapper mapper;
    @Mock private InvestmentServiceClient investmentClient;
    @Mock private BankAccountServiceClient bankAccountClient;

    @InjectMocks
    private NetWorthSyncService service;

    private UUID userId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    // sync

    @Test
    void sync_savesSnapshotWithCorrectNetWorth() {
        BankAccountDTO bank = buildBankAccount("500.00", "EUR");
        InvestmentAccountDTO investmentAccount = buildInvestmentAccount(accountId, "EUR");
        PortfolioSnapshotDTO portfolioSnapshot = buildPortfolioSnapshot("300.00");

        when(bankAccountClient.getBankAccounts(userId)).thenReturn(List.of(bank));
        when(investmentClient.getInvestmentAccounts(userId)).thenReturn(List.of(investmentAccount));
        when(investmentClient.getLatestPortfolioSnapshot(accountId, userId)).thenReturn(portfolioSnapshot);
        when(repository.findByUserIdAndSnapshotDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        service.sync(userId);

        ArgumentCaptor<NetWorthSnapshot> captor = ArgumentCaptor.forClass(NetWorthSnapshot.class);
        verify(repository).save(captor.capture());
        NetWorthSnapshot saved = captor.getValue();

        assertThat(saved.getBankBalanceTotal()).isEqualByComparingTo("500.00");
        assertThat(saved.getInvestmentValueTotal()).isEqualByComparingTo("300.00");
        assertThat(saved.getNetWorth()).isEqualByComparingTo("800.00");
        assertThat(saved.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void sync_updatesExistingSnapshot_whenAlreadySyncedToday() {
        BankAccountDTO bank = buildBankAccount("500.00", "EUR");
        InvestmentAccountDTO investmentAccount = buildInvestmentAccount(accountId, "EUR");
        PortfolioSnapshotDTO portfolioSnapshot = buildPortfolioSnapshot("300.00");

        NetWorthSnapshot existing = new NetWorthSnapshot();
        existing.setUserId(userId);
        existing.setSnapshotDate(LocalDate.now());

        when(bankAccountClient.getBankAccounts(userId)).thenReturn(List.of(bank));
        when(investmentClient.getInvestmentAccounts(userId)).thenReturn(List.of(investmentAccount));
        when(investmentClient.getLatestPortfolioSnapshot(accountId, userId)).thenReturn(portfolioSnapshot);
        when(repository.findByUserIdAndSnapshotDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        service.sync(userId);

        // Same instance updated and saved — not a new entity
        verify(repository).save(existing);
        assertThat(existing.getNetWorth()).isEqualByComparingTo("800.00");
    }

    @Test
    void sync_defaultsBankTotalToZero_whenBankServiceIsDown() {
        InvestmentAccountDTO investmentAccount = buildInvestmentAccount(accountId, "EUR");
        PortfolioSnapshotDTO portfolioSnapshot = buildPortfolioSnapshot("300.00");

        when(bankAccountClient.getBankAccounts(userId)).thenThrow(new RuntimeException("service down"));
        when(investmentClient.getInvestmentAccounts(userId)).thenReturn(List.of(investmentAccount));
        when(investmentClient.getLatestPortfolioSnapshot(accountId, userId)).thenReturn(portfolioSnapshot);
        when(repository.findByUserIdAndSnapshotDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        service.sync(userId);

        ArgumentCaptor<NetWorthSnapshot> captor = ArgumentCaptor.forClass(NetWorthSnapshot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBankBalanceTotal()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getNetWorth()).isEqualByComparingTo("300.00");
    }

    @Test
    void sync_defaultsInvestmentTotalToZero_whenInvestmentServiceIsDown() {
        BankAccountDTO bank = buildBankAccount("500.00", "EUR");

        when(bankAccountClient.getBankAccounts(userId)).thenReturn(List.of(bank));
        when(investmentClient.getInvestmentAccounts(userId)).thenThrow(new RuntimeException("service down"));
        when(repository.findByUserIdAndSnapshotDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        service.sync(userId);

        ArgumentCaptor<NetWorthSnapshot> captor = ArgumentCaptor.forClass(NetWorthSnapshot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getInvestmentValueTotal()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getNetWorth()).isEqualByComparingTo("500.00");
    }

    @Test
    void sync_skipsAccount_whenBalanceIsNull() {
        BankAccountDTO bankWithBalance = buildBankAccount("500.00", "EUR");
        BankAccountDTO bankWithoutBalance = buildBankAccount(null, "EUR");

        when(bankAccountClient.getBankAccounts(userId)).thenReturn(List.of(bankWithBalance, bankWithoutBalance));
        when(investmentClient.getInvestmentAccounts(userId)).thenReturn(List.of());
        when(repository.findByUserIdAndSnapshotDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        service.sync(userId);

        ArgumentCaptor<NetWorthSnapshot> captor = ArgumentCaptor.forClass(NetWorthSnapshot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBankBalanceTotal()).isEqualByComparingTo("500.00");
    }

    // getAll

    @Test
    void getAll_returnsMappedList() {
        NetWorthSnapshot snapshot = new NetWorthSnapshot();
        NetWorthSnapshotResponseDTO dto = new NetWorthSnapshotResponseDTO();

        when(repository.findByUserIdOrderBySnapshotDateAsc(userId)).thenReturn(List.of(snapshot));
        when(mapper.toResponseDTO(snapshot)).thenReturn(dto);

        List<NetWorthSnapshotResponseDTO> result = service.getAll(userId);

        assertThat(result).hasSize(1).containsExactly(dto);
    }

    @Test
    void getAll_returnsEmptyList_whenNoSnapshots() {
        when(repository.findByUserIdOrderBySnapshotDateAsc(userId)).thenReturn(List.of());

        assertThat(service.getAll(userId)).isEmpty();
    }

    // getRange

    @Test
    void getRange_returnsMappedList() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);
        NetWorthSnapshot snapshot = new NetWorthSnapshot();
        NetWorthSnapshotResponseDTO dto = new NetWorthSnapshotResponseDTO();

        when(repository.findByUserIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(userId, from, to))
                .thenReturn(List.of(snapshot));
        when(mapper.toResponseDTO(snapshot)).thenReturn(dto);

        List<NetWorthSnapshotResponseDTO> result = service.getRange(userId, from, to);

        assertThat(result).hasSize(1).containsExactly(dto);
    }

    // getLatest

    @Test
    void getLatest_returnsMappedDTO() {
        NetWorthSnapshot snapshot = new NetWorthSnapshot();
        NetWorthSnapshotResponseDTO dto = new NetWorthSnapshotResponseDTO();

        when(repository.findTopByUserIdOrderBySnapshotDateDesc(userId)).thenReturn(Optional.of(snapshot));
        when(mapper.toResponseDTO(snapshot)).thenReturn(dto);

        assertThat(service.getLatest(userId)).isEqualTo(dto);
    }

    @Test
    void getLatest_throwsException_whenNoSnapshotsExist() {
        when(repository.findTopByUserIdOrderBySnapshotDateDesc(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatest(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No net worth snapshots found");
    }

    // Helpers

    private BankAccountDTO buildBankAccount(String balance, String currency) {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setActive(true);
        dto.setCurrentBalance(balance);
        dto.setCurrency(currency);
        return dto;
    }

    private InvestmentAccountDTO buildInvestmentAccount(UUID id, String currency) {
        InvestmentAccountDTO dto = new InvestmentAccountDTO();
        dto.setId(id.toString());
        dto.setActive(true);
        dto.setCurrency(currency);
        return dto;
    }

    private PortfolioSnapshotDTO buildPortfolioSnapshot(String totalValue) {
        PortfolioSnapshotDTO dto = new PortfolioSnapshotDTO();
        dto.setTotalValue(totalValue);
        return dto;
    }
}