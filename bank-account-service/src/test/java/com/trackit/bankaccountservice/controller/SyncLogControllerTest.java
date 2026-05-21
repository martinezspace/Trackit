package com.trackit.bankaccountservice.controller;

import com.trackit.bankaccountservice.dto.SyncLogResponseDTO;
import com.trackit.bankaccountservice.exception.GlobalExceptionHandler;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.model.SyncTrigger;
import com.trackit.bankaccountservice.service.SyncLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SyncLogController.class)
@Import(GlobalExceptionHandler.class)
public class SyncLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SyncLogService syncService;

    private UUID userId;
    private UUID connectionId;
    private UUID syncLogId;
    private SyncLogResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        connectionId = UUID.randomUUID();
        syncLogId = UUID.randomUUID();

        testResponseDTO = new SyncLogResponseDTO();
        testResponseDTO.setId(syncLogId.toString());
        testResponseDTO.setStatus("RUNNING");
        testResponseDTO.setTrigger("MANUAL");
    }

    // POST /api/bank-connections/{connectionId}/syncs

    @Test
    public void startSync_returns201_withRunningLog() throws Exception {
        when(syncService.startSync(eq(connectionId), eq(userId), eq(SyncTrigger.MANUAL)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(post("/api/bank-connections/{connectionId}/syncs", connectionId)
                        .header("X-User-Id", userId.toString())
                        .param("trigger", "MANUAL"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.trigger").value("MANUAL"));
    }

    @Test
    public void startSync_returns404_whenConnectionNotFound() throws Exception {
        when(syncService.startSync(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Connection not found"));

        mockMvc.perform(post("/api/bank-connections/{connectionId}/syncs", connectionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void startSync_returns400_whenConnectionIsNotActive() throws Exception {
        when(syncService.startSync(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Cannot sync connection with status: EXPIRED"));

        mockMvc.perform(post("/api/bank-connections/{connectionId}/syncs", connectionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isBadRequest());
    }

    // PATCH /api/bank-connections/{connectionId}/syncs/{syncLogId}/complete

    @Test
    public void completeSyncLog_returns200_withCompletedLog() throws Exception {
        SyncLogResponseDTO completedResponse = new SyncLogResponseDTO();
        completedResponse.setStatus("COMPLETED");
        completedResponse.setTransactionsFetched(50);
        completedResponse.setTransactionsNew(10);

        when(syncService.completeSyncLog(eq(syncLogId), eq(50), eq(10), eq(5)))
                .thenReturn(completedResponse);

        mockMvc.perform(patch("/api/bank-connections/{connectionId}/syncs/{syncLogId}/complete",
                        connectionId, syncLogId)
                        .param("fetched", "50")
                        .param("newCount", "10")
                        .param("suggested", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionsFetched").value(50));
    }

    // PATCH /api/bank-connections/{connectionId}/syncs/{syncLogId}/fail

    @Test
    public void failSyncLog_returns200_withFailedLog() throws Exception {
        SyncLogResponseDTO failedResponse = new SyncLogResponseDTO();
        failedResponse.setStatus("FAILED");
        failedResponse.setErrorMessage("GoCardless API timeout");

        when(syncService.failSyncLog(eq(syncLogId), eq("GoCardless API timeout")))
                .thenReturn(failedResponse);

        mockMvc.perform(patch("/api/bank-connections/{connectionId}/syncs/{syncLogId}/fail",
                        connectionId, syncLogId)
                        .param("error", "GoCardless API timeout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("GoCardless API timeout"));
    }

    @Test
    public void failSyncLog_returns404_whenSyncLogNotFound() throws Exception {
        when(syncService.failSyncLog(any(), any()))
                .thenThrow(new ResourceNotFoundException("Sync log not found"));

        mockMvc.perform(patch("/api/bank-connections/{connectionId}/syncs/{syncLogId}/fail",
                        connectionId, syncLogId)
                        .param("error", "timeout"))
                .andExpect(status().isNotFound());
    }
}