package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.SyncLogResponseDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.SyncLogMapper;
import com.trackit.bankaccountservice.model.*;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
import com.trackit.bankaccountservice.repository.SyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class SyncLogServiceTest {

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private BankConnectionRepository bankConnectionRepository;

    @Mock
    private SyncLogMapper syncLogMapper;

    @InjectMocks
    private SyncLogService syncService;

    private UUID userId;
    private UUID connectionId;
    private UUID syncLogId;
    private BankConnection activeConnection;
    private BankConnection expiredConnection;
    private SyncLog testSyncLog;
    private SyncLogResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        connectionId = UUID.randomUUID();
        syncLogId = UUID.randomUUID();

        // Active connection - can be synced
        activeConnection = new BankConnection();
        activeConnection.setUserId(userId);
        activeConnection.setInstitutionId("MONZO_GB");
        activeConnection.setInstitutionName("Monzo");
        activeConnection.setStatus(ConnectionStatus.ACTIVE);

        // Expired connection - cannot be synced
        expiredConnection = new BankConnection();
        expiredConnection.setUserId(userId);
        expiredConnection.setInstitutionId("ING_PL");
        expiredConnection.setInstitutionName("ING");
        expiredConnection.setStatus(ConnectionStatus.EXPIRED);

        // Build reusable test sync log
        testSyncLog = new SyncLog();
        testSyncLog.setConnection(activeConnection);
        testSyncLog.setTrigger(SyncTrigger.MANUAL);
        testSyncLog.setStatus(SyncStatus.RUNNING);

        // Build reusable test response DTO
        testResponseDTO = new SyncLogResponseDTO();
        testResponseDTO.setId(syncLogId.toString());
        testResponseDTO.setStatus("RUNNING");
        testResponseDTO.setTrigger("MANUAL");
    }

    // startSync

    @Test
    public void startSync_throwsException_whenConnectionNotFound() {
        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> syncService.startSync(connectionId, userId, SyncTrigger.MANUAL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Connection not found");

        verify(syncLogRepository, never()).save(any());
    }

    @Test
    public void startSync_throwsException_whenConnectionIsNotActive() {
        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.of(expiredConnection));

        assertThatThrownBy(() -> syncService.startSync(connectionId, userId, SyncTrigger.MANUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot sync connection with status");

        verify(syncLogRepository, never()).save(any());
    }

    @Test
    public void startSync_createsRunningLog_whenConnectionIsActive() {
        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.of(activeConnection));
        when(syncLogRepository.save(any())).thenReturn(testSyncLog);
        when(syncLogMapper.toResponseDTO(testSyncLog)).thenReturn(testResponseDTO);

        SyncLogResponseDTO result = syncService.startSync(connectionId, userId, SyncTrigger.MANUAL);

        assertThat(result.getStatus()).isEqualTo("RUNNING");
        assertThat(result.getTrigger()).isEqualTo("MANUAL");
        verify(syncLogRepository, times(1)).save(argThat(log ->
                log.getStatus() == SyncStatus.RUNNING &&
                        log.getTrigger() == SyncTrigger.MANUAL
        ));
    }

    // completeSyncLog

    @Test
    public void completeSyncLog_throwsException_whenSyncLogNotFound() {
        when(syncLogRepository.findById(syncLogId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syncService.completeSyncLog(syncLogId, 10, 5, 3))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sync log not found");
    }

    @Test
    public void completeSyncLog_updatesStatusAndCounts() {
        when(syncLogRepository.findById(syncLogId)).thenReturn(Optional.of(testSyncLog));
        when(syncLogRepository.save(testSyncLog)).thenReturn(testSyncLog);
        when(bankConnectionRepository.save(activeConnection)).thenReturn(activeConnection);
        when(syncLogMapper.toResponseDTO(testSyncLog)).thenReturn(testResponseDTO);

        syncService.completeSyncLog(syncLogId, 50, 10, 5);

        assertThat(testSyncLog.getStatus()).isEqualTo(SyncStatus.COMPLETED);
        assertThat(testSyncLog.getTransactionsFetched()).isEqualTo(50);
        assertThat(testSyncLog.getTransactionsNew()).isEqualTo(10);
        assertThat(testSyncLog.getTransactionsSuggested()).isEqualTo(5);
        assertThat(testSyncLog.getCompletedAt()).isNotNull();
        verify(syncLogRepository, times(1)).save(testSyncLog);
    }

    @Test
    public void completeSyncLog_updatesConnectionLastSyncedAt() {
        when(syncLogRepository.findById(syncLogId)).thenReturn(Optional.of(testSyncLog));
        when(syncLogRepository.save(testSyncLog)).thenReturn(testSyncLog);
        when(bankConnectionRepository.save(activeConnection)).thenReturn(activeConnection);
        when(syncLogMapper.toResponseDTO(testSyncLog)).thenReturn(testResponseDTO);

        syncService.completeSyncLog(syncLogId, 50, 10, 5);

        // lastSyncedAt is used by incremental sync to know where to start next time
        assertThat(activeConnection.getLastSyncedAt()).isNotNull();
        verify(bankConnectionRepository, times(1)).save(activeConnection);
    }

    // failSyncLog

    @Test
    public void failSyncLog_throwsException_whenSyncLogNotFound() {
        when(syncLogRepository.findById(syncLogId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> syncService.failSyncLog(syncLogId, "timeout"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sync log not found");
    }

    @Test
    public void failSyncLog_setsStatusAndErrorMessage() {
        when(syncLogRepository.findById(syncLogId)).thenReturn(Optional.of(testSyncLog));
        when(syncLogRepository.save(testSyncLog)).thenReturn(testSyncLog);
        when(syncLogMapper.toResponseDTO(testSyncLog)).thenReturn(testResponseDTO);

        syncService.failSyncLog(syncLogId, "GoCardless API timeout after 30s");

        assertThat(testSyncLog.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(testSyncLog.getErrorMessage()).isEqualTo("GoCardless API timeout after 30s");
        assertThat(testSyncLog.getCompletedAt()).isNotNull();
        verify(syncLogRepository, times(1)).save(testSyncLog);
    }
}