package com.trackit.bankaccountservice.mapper;

import com.trackit.bankaccountservice.dto.SyncLogResponseDTO;
import com.trackit.bankaccountservice.model.SyncLog;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class SyncLogMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Entity => ResponseDTO
    // SyncLog is read-only from the API - created and updated by sync logic internally
    public SyncLogResponseDTO toResponseDTO(SyncLog syncLog) {
        SyncLogResponseDTO response = new SyncLogResponseDTO();
        response.setId(syncLog.getId().toString());
        response.setConnectionId(syncLog.getConnection().getId().toString());
        response.setAccountId(syncLog.getAccount() != null
                ? syncLog.getAccount().getId().toString() : null);
        response.setTrigger(syncLog.getTrigger().name());
        response.setStatus(syncLog.getStatus().name());
        response.setTransactionsFetched(syncLog.getTransactionsFetched());
        response.setTransactionsNew(syncLog.getTransactionsNew());
        response.setTransactionsSuggested(syncLog.getTransactionsSuggested());
        response.setErrorMessage(syncLog.getErrorMessage());
        response.setStartedAt(syncLog.getStartedAt() != null
                ? syncLog.getStartedAt().format(DATE_FORMAT) : null);
        response.setCompletedAt(syncLog.getCompletedAt() != null
                ? syncLog.getCompletedAt().format(DATE_FORMAT) : null);
        return response;
    }
}