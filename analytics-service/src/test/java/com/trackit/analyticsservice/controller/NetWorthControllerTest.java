package com.trackit.analyticsservice.controller;

import com.trackit.analyticsservice.dto.response.NetWorthSnapshotResponseDTO;
import com.trackit.analyticsservice.exception.ResourceNotFoundException;
import com.trackit.analyticsservice.service.NetWorthSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.trackit.analyticsservice.exception.GlobalExceptionHandler;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NetWorthController.class)
@Import(GlobalExceptionHandler.class)
class NetWorthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NetWorthSyncService service;

    private UUID userId;
    private NetWorthSnapshotResponseDTO testDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testDTO = new NetWorthSnapshotResponseDTO();
        testDTO.setId(UUID.randomUUID().toString());
        testDTO.setUserId(userId.toString());
        testDTO.setSnapshotDate("2024-03-01");
        testDTO.setBankBalanceTotal("500.00");
        testDTO.setInvestmentValueTotal("300.00");
        testDTO.setNetWorth("800.00");
        testDTO.setCurrency("EUR");
    }

    // GET /api/analytics/net-worth

    @Test
    void getAll_returns200WithList() throws Exception {
        when(service.getAll(userId)).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/analytics/net-worth")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].netWorth").value("800.00"))
                .andExpect(jsonPath("$[0].currency").value("EUR"));
    }

    @Test
    void getAll_returns200WithEmptyList_whenNoSnapshots() throws Exception {
        when(service.getAll(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/net-worth")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // GET /api/analytics/net-worth/latest

    @Test
    void getLatest_returns200WithDTO() throws Exception {
        when(service.getLatest(userId)).thenReturn(testDTO);

        mockMvc.perform(get("/api/analytics/net-worth/latest")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netWorth").value("800.00"))
                .andExpect(jsonPath("$.snapshotDate").value("2024-03-01"));
    }

    @Test
    void getLatest_returns404_whenNoSnapshotsExist() throws Exception {
        when(service.getLatest(userId))
                .thenThrow(new ResourceNotFoundException("No net worth snapshots found for user " + userId));

        mockMvc.perform(get("/api/analytics/net-worth/latest")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // GET /api/analytics/net-worth/range

    @Test
    void getRange_returns200WithList() throws Exception {
        when(service.getRange(eq(userId), any(), any())).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/analytics/net-worth/range")
                        .header("X-User-Id", userId.toString())
                        .param("from", "2024-01-01")
                        .param("to", "2024-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].netWorth").value("800.00"));
    }

    @Test
    void getRange_returns400_whenFromParamMissing() throws Exception {
        mockMvc.perform(get("/api/analytics/net-worth/range")
                        .header("X-User-Id", userId.toString())
                        .param("to", "2024-03-31"))
                .andExpect(status().isBadRequest());
    }

    // POST /api/analytics/net-worth/sync

    @Test
    void sync_returns204() throws Exception {
        doNothing().when(service).sync(userId);

        mockMvc.perform(post("/api/analytics/net-worth/sync")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());
    }
}