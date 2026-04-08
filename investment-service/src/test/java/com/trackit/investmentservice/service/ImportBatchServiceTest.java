package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.ImportBatchCreateDTO;
import com.trackit.investmentservice.dto.ImportBatchResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.ImportBatchMapper;
import com.trackit.investmentservice.model.*;
import com.trackit.investmentservice.repository.ImportBatchRepository;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportBatchServiceTest {

    @Mock
    private ImportBatchRepository importBatchRepository;

    @Mock
    private InvestmentAccountRepository investmentAccountRepository;

    @Mock
    private ImportBatchMapper importBatchMapper;

    @InjectMocks
    private ImportBatchService importBatchService;

    private UUID userId;
    private UUID accountId;
    private UUID batchId;
    private InvestmentAccount testAccount;
    private ImportBatch testBatch;
    private ImportBatchResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        batchId = UUID.randomUUID();

        testAccount = new InvestmentAccount();
        testAccount.setUserId(userId);
        testAccount.setBrokerName("Trading212");
        testAccount.setAccountType(AccountType.BROKERAGE);
        testAccount.setCurrency("PLN");

        testBatch = new ImportBatch();
        testBatch.setAccount(testAccount);
        testBatch.setBrokerFormat(BrokerFormat.TRADING212_STANDARD);
        testBatch.setFilename("from_2024-01-01_to_2024-12-31.csv");
        testBatch.setStatus(ImportStatus.PENDING);

        testResponseDTO = new ImportBatchResponseDTO();
        testResponseDTO.setId(batchId.toString());
        testResponseDTO.setAccountId(accountId.toString());
        testResponseDTO.setBrokerFormat("TRADING212_STANDARD");
        testResponseDTO.setFilename("from_2024-01-01_to_2024-12-31.csv");
        testResponseDTO.setStatus("PENDING");
    }

    // getAllBatchesForAccount

    @Test
    void getAllBatchesForAccount_returnsList_whenAccountExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.findByAccount_IdOrderByCreatedAtDesc(accountId))
                .thenReturn(List.of(testBatch));
        when(importBatchMapper.toResponseDTO(testBatch))
                .thenReturn(testResponseDTO);

        List<ImportBatchResponseDTO> result = importBatchService.getAllBatchesForAccount(accountId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getAllBatchesForAccount_throwsException_whenAccountNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> importBatchService.getAllBatchesForAccount(accountId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account Not Found");
    }

    // getBatchById

    @Test
    void getBatchById_returnsDTO_whenBatchExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.findByIdAndAccount_Id(batchId, accountId))
                .thenReturn(Optional.of(testBatch));
        when(importBatchMapper.toResponseDTO(testBatch))
                .thenReturn(testResponseDTO);

        ImportBatchResponseDTO result = importBatchService.getBatchById(batchId, accountId, userId);

        assertThat(result.getBrokerFormat()).isEqualTo("TRADING212_STANDARD");
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getBatchById_throwsException_whenBatchNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.findByIdAndAccount_Id(batchId, accountId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> importBatchService.getBatchById(batchId, accountId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Import Batch Not Found");
    }

    // createBatch

    @Test
    void createBatch_savesAndReturnsDTO_whenNoActiveImport() {
        ImportBatchCreateDTO createDTO = new ImportBatchCreateDTO();
        createDTO.setAccountId(accountId);
        createDTO.setBrokerFormat(BrokerFormat.TRADING212_STANDARD);
        createDTO.setFilename("from_2024-01-01_to_2024-12-31.csv");

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.existsByAccount_IdAndStatusIn(eq(accountId), anyList()))
                .thenReturn(false);
        when(importBatchRepository.save(any(ImportBatch.class)))
                .thenReturn(testBatch);
        when(importBatchMapper.toResponseDTO(testBatch))
                .thenReturn(testResponseDTO);

        ImportBatchResponseDTO result = importBatchService.createBatch(createDTO, userId);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getBrokerFormat()).isEqualTo("TRADING212_STANDARD");
        verify(importBatchRepository, times(1)).save(any(ImportBatch.class));
    }

    @Test
    void createBatch_throwsException_whenActiveImportExists() {
        ImportBatchCreateDTO createDTO = new ImportBatchCreateDTO();
        createDTO.setAccountId(accountId);
        createDTO.setBrokerFormat(BrokerFormat.TRADING212_STANDARD);
        createDTO.setFilename("from_2024-01-01_to_2024-12-31.csv");

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.existsByAccount_IdAndStatusIn(eq(accountId), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> importBatchService.createBatch(createDTO, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("An import is already in progress");

        verify(importBatchRepository, never()).save(any());
    }

    @Test
    void createBatch_throwsException_whenAccountNotFound() {
        ImportBatchCreateDTO createDTO = new ImportBatchCreateDTO();
        createDTO.setAccountId(accountId);
        createDTO.setBrokerFormat(BrokerFormat.TRADING212_STANDARD);
        createDTO.setFilename("test.csv");

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> importBatchService.createBatch(createDTO, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account Not Found");
    }

    // cancelBatch

    @Test
    void cancelBatch_setsCancelledStatus_whenBatchIsPending() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.findByIdAndAccount_Id(batchId, accountId))
                .thenReturn(Optional.of(testBatch));

        ImportBatchResponseDTO cancelledResponse = new ImportBatchResponseDTO();
        cancelledResponse.setStatus("CANCELLED");

        when(importBatchRepository.save(testBatch))
                .thenReturn(testBatch);
        when(importBatchMapper.toResponseDTO(testBatch))
                .thenReturn(cancelledResponse);

        ImportBatchResponseDTO result = importBatchService.cancelBatch(batchId, accountId, userId);

        assertThat(testBatch.getStatus()).isEqualTo(ImportStatus.CANCELLED);
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(importBatchRepository, times(1)).save(testBatch);
    }

    @Test
    void cancelBatch_throwsException_whenBatchAlreadyCompleted() {
        testBatch.setStatus(ImportStatus.COMPLETED);

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.findByIdAndAccount_Id(batchId, accountId))
                .thenReturn(Optional.of(testBatch));

        assertThatThrownBy(() -> importBatchService.cancelBatch(batchId, accountId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a batch with status");

        verify(importBatchRepository, never()).save(any());
    }

    @Test
    void cancelBatch_throwsException_whenBatchAlreadyCancelled() {
        testBatch.setStatus(ImportStatus.CANCELLED);

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.findByIdAndAccount_Id(batchId, accountId))
                .thenReturn(Optional.of(testBatch));

        assertThatThrownBy(() -> importBatchService.cancelBatch(batchId, accountId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a batch with status");
    }
}