package com.trackit.investmentservice.service;

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
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ImportServiceTest {

    @Mock private FileStorageService fileStorageService;
    @Mock private ImportBatchRepository importBatchRepository;
    @Mock private InvestmentAccountRepository investmentAccountRepository;
    @Mock private ImportBatchMapper importBatchMapper;
    @Mock private ImportProcessingService importProcessingService;
    @Mock private MultipartFile multipartFile;

    @InjectMocks
    private ImportService importService;

    private UUID userId;
    private UUID accountId;
    private InvestmentAccount testAccount;
    private ImportBatch testBatch;
    private ImportBatchResponseDTO testResponseDTO;

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

        testBatch = new ImportBatch();
        testBatch.setAccount(testAccount);
        testBatch.setBrokerFormat(BrokerFormat.TRADING212_STANDARD);
        testBatch.setStatus(ImportStatus.PENDING);

        testResponseDTO = new ImportBatchResponseDTO();
        testResponseDTO.setStatus("PENDING");

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getSize()).thenReturn(1000L);
    }

    // initiateImport
    @Test
    void initiateImport_uploadsFileCreatesAndReturnsBatch() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.existsByAccount_IdAndStatusIn(
                eq(accountId), any()))
                .thenReturn(false);
        when(fileStorageService.uploadFile(any(), any(), any()))
                .thenReturn("imports/test.csv");
        when(importBatchRepository.save(any())).thenReturn(testBatch);
        when(importBatchMapper.toResponseDTO(testBatch)).thenReturn(testResponseDTO);

        ImportBatchResponseDTO result = importService.initiateImport(
                userId, accountId, BrokerFormat.TRADING212_STANDARD, multipartFile);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(fileStorageService, times(1)).uploadFile(any(), any(), any());
        verify(importBatchRepository, times(1)).save(any());
        // Verify async processing was triggered
        verify(importProcessingService, times(1))
                .processImportAsync(any(), any(), any(), any());
    }

    @Test
    void initiateImport_throwsException_whenAccountNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> importService.initiateImport(
                userId, accountId, BrokerFormat.TRADING212_STANDARD, multipartFile))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");

        verifyNoInteractions(fileStorageService);
        verifyNoInteractions(importBatchRepository);
    }

    @Test
    void initiateImport_throwsException_whenImportAlreadyInProgress() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.existsByAccount_IdAndStatusIn(
                eq(accountId), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> importService.initiateImport(
                userId, accountId, BrokerFormat.TRADING212_STANDARD, multipartFile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("import is already in progress");

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void initiateImport_doesNotBlockOnAsyncProcessing() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(importBatchRepository.existsByAccount_IdAndStatusIn(any(), any()))
                .thenReturn(false);
        when(fileStorageService.uploadFile(any(), any(), any()))
                .thenReturn("imports/test.csv");
        when(importBatchRepository.save(any())).thenReturn(testBatch);
        when(importBatchMapper.toResponseDTO(testBatch)).thenReturn(testResponseDTO);

        // Should return immediately — not wait for async processing
        doNothing().when(importProcessingService)
                .processImportAsync(any(), any(), any(), any());

        ImportBatchResponseDTO result = importService.initiateImport(
                userId, accountId, BrokerFormat.TRADING212_STANDARD, multipartFile);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }
}