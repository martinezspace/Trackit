package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.PagedResponseDTO;
import com.trackit.bankaccountservice.dto.SyncLogResponseDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.SyncLogMapper;
import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.model.ConnectionStatus;
import com.trackit.bankaccountservice.model.SyncLog;
import com.trackit.bankaccountservice.model.SyncStatus;
import com.trackit.bankaccountservice.model.SyncTrigger;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
import com.trackit.bankaccountservice.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class SyncLogService {

    private final SyncLogRepository syncLogRepository;
    private final BankConnectionRepository connectionRepository;
    private final SyncLogMapper mapper;

    // Queries

    public PagedResponseDTO<SyncLogResponseDTO> getSyncLogsForConnection(UUID connectionId, Pageable pageable) {
        return PagedResponseDTO.from(
                syncLogRepository.findByConnectionIdOrderByStartedAtDesc(connectionId, pageable)
                        .map(mapper::toResponseDTO));
    }

    // Commands

    public SyncLogResponseDTO startSync(UUID connectionId, UUID userId, SyncTrigger trigger) {
        BankConnection connection = connectionRepository.findByIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found: " + connectionId));

        // Only active connections can be synced
        if (connection.getStatus() != ConnectionStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Cannot sync connection with status: " + connection.getStatus());
        }

        SyncLog syncLog = new SyncLog();
        syncLog.setConnection(connection);
        syncLog.setTrigger(trigger);
        syncLog.setStatus(SyncStatus.RUNNING);

        return mapper.toResponseDTO(syncLogRepository.save(syncLog));
    }

    @Transactional
    public SyncLogResponseDTO completeSyncLog(UUID syncLogId, int fetched, int newCount, int suggested) {
        SyncLog syncLog = syncLogRepository.findById(syncLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Sync log not found: " + syncLogId));

        syncLog.setStatus(SyncStatus.COMPLETED);
        syncLog.setTransactionsFetched(fetched);
        syncLog.setTransactionsNew(newCount);
        syncLog.setTransactionsSuggested(suggested);
        syncLog.setCompletedAt(LocalDateTime.now());

        // Update lastSyncedAt so next sync knows where to start from
        BankConnection connection = syncLog.getConnection();
        connection.setLastSyncedAt(LocalDateTime.now());
        connectionRepository.save(connection);

        return mapper.toResponseDTO(syncLogRepository.save(syncLog));
    }

    public SyncLogResponseDTO failSyncLog(UUID syncLogId, String errorMessage) {
        SyncLog syncLog = syncLogRepository.findById(syncLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Sync log not found: " + syncLogId));

        syncLog.setStatus(SyncStatus.FAILED);
        syncLog.setErrorMessage(errorMessage);
        syncLog.setCompletedAt(LocalDateTime.now());

        return mapper.toResponseDTO(syncLogRepository.save(syncLog));
    }
}