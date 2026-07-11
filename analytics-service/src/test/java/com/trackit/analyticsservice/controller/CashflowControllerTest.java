package com.trackit.analyticsservice.controller;

import com.trackit.analyticsservice.dto.response.CashflowSummaryResponseDTO;
import com.trackit.analyticsservice.exception.GlobalExceptionHandler;
import com.trackit.analyticsservice.exception.ResourceNotFoundException;
import com.trackit.analyticsservice.service.CashflowSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashflowController.class)
@Import(GlobalExceptionHandler.class)
class CashflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CashflowSyncService service;

    private UUID userId;
    private CashflowSummaryResponseDTO testDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testDTO = new CashflowSummaryResponseDTO();
        testDTO.setId(UUID.randomUUID().toString());
        testDTO.setUserId(userId.toString());
        testDTO.setPeriodYear(2024);
        testDTO.setPeriodMonth(3);
        testDTO.setTotalIncome("1500.00");
        testDTO.setTotalExpenses("300.00");
        testDTO.setNetCashflow("1200.00");
        testDTO.setTransactionCount(3);
        testDTO.setCurrency("EUR");
    }

    // GET /api/analytics/cashflow

    @Test
    void getAll_returns200WithList() throws Exception {
        when(service.getAll(userId)).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/analytics/cashflow")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalIncome").value("1500.00"))
                .andExpect(jsonPath("$[0].netCashflow").value("1200.00"));
    }

    @Test
    void getAll_returns200WithEmptyList_whenNoSummaries() throws Exception {
        when(service.getAll(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/cashflow")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // GET /api/analytics/cashflow/{year}/{month}

    @Test
    void getByPeriod_returns200WithDTO() throws Exception {
        when(service.getByPeriod(userId, 2024, 3)).thenReturn(testDTO);

        mockMvc.perform(get("/api/analytics/cashflow/2024/3")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value("1500.00"))
                .andExpect(jsonPath("$.periodMonth").value(3));
    }

    @Test
    void getByPeriod_returns404_whenPeriodNotFound() throws Exception {
        when(service.getByPeriod(userId, 2024, 3))
                .thenThrow(new ResourceNotFoundException("No cashflow summary found"));

        mockMvc.perform(get("/api/analytics/cashflow/2024/3")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // POST /api/analytics/cashflow/sync/{year}/{month}

    @Test
    void sync_returns204() throws Exception {
        doNothing().when(service).sync(userId, 2024, 3);

        mockMvc.perform(post("/api/analytics/cashflow/sync/2024/3")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());
    }
}