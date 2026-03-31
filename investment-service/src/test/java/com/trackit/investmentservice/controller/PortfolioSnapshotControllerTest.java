package com.trackit.investmentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.investmentservice.dto.PortfolioSnapshotCreateDTO;
import com.trackit.investmentservice.dto.PortfolioSnapshotResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.service.PortfolioSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioSnapshotController.class)
@TestPropertySource(properties = "internal.key=internal-service-key")
class PortfolioSnapshotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PortfolioSnapshotService snapshotService;

    private UUID userId;
    private UUID accountId;
    private PortfolioSnapshotResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testResponseDTO = new PortfolioSnapshotResponseDTO();
        testResponseDTO.setAccountId(accountId.toString());
        testResponseDTO.setSnapshotDate("2024-03-31");
        testResponseDTO.setTotalValue("10800.00");
        testResponseDTO.setTotalInvested("10500.00");
        testResponseDTO.setTotalGainLoss("300.00");
        testResponseDTO.setGainLossPct("2.8571");
        testResponseDTO.setCurrency("PLN");
    }

    // GET /api/portfolio-snapshots?accountId=
    @Test
    void getAllSnapshots_returns200WithList() throws Exception {
        when(snapshotService.getAllSnapshots(accountId, userId))
                .thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/portfolio-snapshots")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalValue").value("10800.00"))
                .andExpect(jsonPath("$[0].gainLossPct").value("2.8571"));
    }

    @Test
    void getAllSnapshots_returns404_whenAccountNotFound() throws Exception {
        when(snapshotService.getAllSnapshots(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account Not Found"));

        mockMvc.perform(get("/api/portfolio-snapshots")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // GET /api/portfolio-snapshots/range
    @Test
    void getSnapshotsByDateRange_returns200WithList() throws Exception {
        when(snapshotService.getSnapshotsByDateRange(
                eq(accountId), eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/portfolio-snapshots/range")
                        .param("accountId", accountId.toString())
                        .param("from", "2024-01-01")
                        .param("to", "2024-03-31")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snapshotDate").value("2024-03-31"));
    }

    // GET /api/portfolio-snapshots/latest
    @Test
    void getLatestSnapshot_returns200WithDTO() throws Exception {
        when(snapshotService.getLatestSnapshot(accountId, userId))
                .thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/portfolio-snapshots/latest")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value("10800.00"))
                .andExpect(jsonPath("$.totalGainLoss").value("300.00"));
    }

    @Test
    void getLatestSnapshot_returns404_whenNoSnapshotsExist() throws Exception {
        when(snapshotService.getLatestSnapshot(accountId, userId))
                .thenThrow(new ResourceNotFoundException("No Snapshots Found For Account"));

        mockMvc.perform(get("/api/portfolio-snapshots/latest")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // POST /api/portfolio-snapshots
    @Test
    void saveSnapshot_returns201_withValidKey() throws Exception {
        PortfolioSnapshotCreateDTO createDTO = new PortfolioSnapshotCreateDTO();
        createDTO.setAccountId(accountId);
        createDTO.setSnapshotDate(LocalDate.of(2024, 3, 31));
        createDTO.setTotalValue(new BigDecimal("10800.00"));
        createDTO.setTotalInvested(new BigDecimal("10500.00"));
        createDTO.setTotalGainLoss(new BigDecimal("300.00"));
        createDTO.setGainLossPct(new BigDecimal("2.8571"));
        createDTO.setCurrency("PLN");

        when(snapshotService.saveSnapshot(any(PortfolioSnapshotCreateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(post("/api/portfolio-snapshots")
                        .header("X-Internal-Key", "internal-service-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalValue").value("10800.00"));
    }

    @Test
    void saveSnapshot_returns403_withInvalidKey() throws Exception {
        PortfolioSnapshotCreateDTO createDTO = new PortfolioSnapshotCreateDTO();
        createDTO.setAccountId(accountId);
        createDTO.setSnapshotDate(LocalDate.of(2024, 3, 31));
        createDTO.setTotalValue(new BigDecimal("10800.00"));
        createDTO.setTotalInvested(new BigDecimal("10500.00"));
        createDTO.setTotalGainLoss(new BigDecimal("300.00"));
        createDTO.setGainLossPct(new BigDecimal("2.8571"));
        createDTO.setCurrency("PLN");

        mockMvc.perform(post("/api/portfolio-snapshots")
                        .header("X-Internal-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveSnapshot_returns400_whenAccountIdMissing() throws Exception {
        PortfolioSnapshotCreateDTO createDTO = new PortfolioSnapshotCreateDTO();
        // accountId intentionally missing
        createDTO.setSnapshotDate(LocalDate.of(2024, 3, 31));
        createDTO.setTotalValue(new BigDecimal("10800.00"));
        createDTO.setTotalInvested(new BigDecimal("10500.00"));
        createDTO.setTotalGainLoss(new BigDecimal("300.00"));
        createDTO.setGainLossPct(new BigDecimal("2.8571"));
        createDTO.setCurrency("PLN");

        mockMvc.perform(post("/api/portfolio-snapshots")
                        .header("X-Internal-Key", "internal-service-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }
}