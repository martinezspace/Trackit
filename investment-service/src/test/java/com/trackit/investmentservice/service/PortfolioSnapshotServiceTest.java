package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.PortfolioSnapshotCreateDTO;
import com.trackit.investmentservice.dto.PortfolioSnapshotResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.PortfolioSnapshotMapper;
import com.trackit.investmentservice.model.AccountType;
import com.trackit.investmentservice.model.InvestmentAccount;
import com.trackit.investmentservice.model.PortfolioSnapshot;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import com.trackit.investmentservice.repository.PortfolioSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class PortfolioSnapshotServiceTest {

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    @Mock
    private InvestmentAccountRepository investmentAccountRepository;

    @Mock
    private PortfolioSnapshotMapper snapshotMapper;

    @InjectMocks
    private PortfolioSnapshotService snapshotService;

    private UUID userId;
    private UUID accountId;
    private InvestmentAccount testAccount;
    private PortfolioSnapshot testSnapshot;
    private PortfolioSnapshotResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testAccount = new InvestmentAccount();
        testAccount.setUserId(userId);
        testAccount.setBrokerName("Trading212");
        testAccount.setAccountType(AccountType.BROKERAGE);
        testAccount.setCurrency("PLN");

        testSnapshot = new PortfolioSnapshot();
        testSnapshot.setAccount(testAccount);
        testSnapshot.setSnapshotDate(LocalDate.of(2024, 3, 15));
        testSnapshot.setTotalValue(new BigDecimal("11200.00"));
        testSnapshot.setTotalInvested(new BigDecimal("10000.00"));
        testSnapshot.setTotalGainLoss(new BigDecimal("1200.00"));
        testSnapshot.setGainLossPct(new BigDecimal("12.0000"));
        testSnapshot.setCurrency("PLN");

        testResponseDTO = new PortfolioSnapshotResponseDTO();
        testResponseDTO.setAccountId(accountId.toString());
        testResponseDTO.setSnapshotDate("2024-03-15");
        testResponseDTO.setTotalValue("11200.00");
        testResponseDTO.setTotalInvested("10000.00");
        testResponseDTO.setTotalGainLoss("1200.00");
        testResponseDTO.setGainLossPct("12.0000");
        testResponseDTO.setCurrency("PLN");
    }

    // getAllSnapshots
    @Test
    void getAllSnapshots_returnsList_whenAccountExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(snapshotRepository.findByAccount_IdOrderBySnapshotDateAsc(accountId))
                .thenReturn(List.of(testSnapshot));
        when(snapshotMapper.toResponseDTO(testSnapshot))
                .thenReturn(testResponseDTO);

        List<PortfolioSnapshotResponseDTO> result = snapshotService.getAllSnapshots(accountId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalValue()).isEqualTo("11200.00");
        assertThat(result.get(0).getGainLossPct()).isEqualTo("12.0000");
    }

    @Test
    void getAllSnapshots_returnsEmptyList_whenNoSnapshots() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(snapshotRepository.findByAccount_IdOrderBySnapshotDateAsc(accountId))
                .thenReturn(List.of());

        List<PortfolioSnapshotResponseDTO> result = snapshotService.getAllSnapshots(accountId, userId);

        assertThat(result).isEmpty();
    }

    @Test
    void getAllSnapshots_throwsException_whenAccountNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.getAllSnapshots(accountId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account Not Found");
    }

    // getSnapshotsByDateRange
    @Test
    void getSnapshotsByDateRange_returnsList_forDateRange() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(snapshotRepository.findByAccount_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                accountId, from, to))
                .thenReturn(List.of(testSnapshot));
        when(snapshotMapper.toResponseDTO(testSnapshot))
                .thenReturn(testResponseDTO);

        List<PortfolioSnapshotResponseDTO> result =
                snapshotService.getSnapshotsByDateRange(accountId, userId, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSnapshotDate()).isEqualTo("2024-03-15");
    }

    @Test
    void getSnapshotsByDateRange_returnsEmpty_whenNoSnapshotsInRange() {
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(snapshotRepository.findByAccount_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                accountId, from, to))
                .thenReturn(List.of());

        List<PortfolioSnapshotResponseDTO> result =
                snapshotService.getSnapshotsByDateRange(accountId, userId, from, to);

        assertThat(result).isEmpty();
    }

    // getLatestSnapshot
    @Test
    void getLatestSnapshot_returnsDTO_whenSnapshotExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(snapshotRepository.findTopByAccount_IdOrderBySnapshotDateDesc(accountId))
                .thenReturn(Optional.of(testSnapshot));
        when(snapshotMapper.toResponseDTO(testSnapshot))
                .thenReturn(testResponseDTO);

        PortfolioSnapshotResponseDTO result = snapshotService.getLatestSnapshot(accountId, userId);

        assertThat(result.getTotalValue()).isEqualTo("11200.00");
        assertThat(result.getTotalGainLoss()).isEqualTo("1200.00");
    }

    @Test
    void getLatestSnapshot_throwsException_whenNoSnapshotsExist() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(snapshotRepository.findTopByAccount_IdOrderBySnapshotDateDesc(accountId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.getLatestSnapshot(accountId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No Snapshots Found For Account");
    }

    // saveSnapshot
    @Test
    void saveSnapshot_savesAndReturnsDTO_whenNoExistingSnapshot() {
        PortfolioSnapshotCreateDTO createDTO = new PortfolioSnapshotCreateDTO();
        createDTO.setAccountId(accountId);
        createDTO.setSnapshotDate(LocalDate.of(2024, 3, 15));
        createDTO.setTotalValue(new BigDecimal("11200.00"));
        createDTO.setTotalInvested(new BigDecimal("10000.00"));
        createDTO.setTotalGainLoss(new BigDecimal("1200.00"));
        createDTO.setGainLossPct(new BigDecimal("12.0000"));
        createDTO.setCurrency("PLN");

        when(investmentAccountRepository.findById(accountId))
                .thenReturn(Optional.of(testAccount));
        when(snapshotRepository.existsByAccount_IdAndSnapshotDate(
                accountId, createDTO.getSnapshotDate()))
                .thenReturn(false);
        when(snapshotMapper.toEntity(createDTO, testAccount))
                .thenReturn(testSnapshot);
        when(snapshotRepository.save(testSnapshot))
                .thenReturn(testSnapshot);
        when(snapshotMapper.toResponseDTO(testSnapshot))
                .thenReturn(testResponseDTO);

        PortfolioSnapshotResponseDTO result = snapshotService.saveSnapshot(createDTO);

        assertThat(result.getTotalValue()).isEqualTo("11200.00");
        verify(snapshotRepository, times(1)).save(testSnapshot);
    }

    @Test
    void saveSnapshot_skipsInsert_whenSnapshotAlreadyExists() {
        PortfolioSnapshotCreateDTO createDTO = new PortfolioSnapshotCreateDTO();
        createDTO.setAccountId(accountId);
        createDTO.setSnapshotDate(LocalDate.of(2024, 3, 15));
        createDTO.setTotalValue(new BigDecimal("11200.00"));
        createDTO.setTotalInvested(new BigDecimal("10000.00"));
        createDTO.setTotalGainLoss(new BigDecimal("1200.00"));
        createDTO.setGainLossPct(new BigDecimal("12.0000"));
        createDTO.setCurrency("PLN");

        when(investmentAccountRepository.findById(accountId))
                .thenReturn(Optional.of(testAccount));
        when(snapshotRepository.existsByAccount_IdAndSnapshotDate(
                accountId, createDTO.getSnapshotDate()))
                .thenReturn(true);
        when(snapshotRepository.findTopByAccount_IdOrderBySnapshotDateDesc(accountId))
                .thenReturn(Optional.of(testSnapshot));
        when(snapshotMapper.toResponseDTO(testSnapshot))
                .thenReturn(testResponseDTO);

        snapshotService.saveSnapshot(createDTO);

        verify(snapshotRepository, never()).save(any());
    }

    @Test
    void saveSnapshot_throwsException_whenAccountNotFound() {
        PortfolioSnapshotCreateDTO createDTO = new PortfolioSnapshotCreateDTO();
        createDTO.setAccountId(accountId);
        createDTO.setSnapshotDate(LocalDate.of(2024, 3, 15));
        createDTO.setTotalValue(new BigDecimal("11200.00"));
        createDTO.setTotalInvested(new BigDecimal("10000.00"));
        createDTO.setTotalGainLoss(new BigDecimal("1200.00"));
        createDTO.setGainLossPct(new BigDecimal("12.0000"));
        createDTO.setCurrency("PLN");

        when(investmentAccountRepository.findById(accountId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotService.saveSnapshot(createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account Not Found");
    }
}