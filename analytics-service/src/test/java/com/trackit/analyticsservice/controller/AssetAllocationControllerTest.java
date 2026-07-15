package com.trackit.analyticsservice.controller;

import com.trackit.analyticsservice.dto.response.AssetAllocationResponseDTO;
import com.trackit.analyticsservice.exception.GlobalExceptionHandler;
import com.trackit.analyticsservice.service.AssetAllocationSyncService;
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

@WebMvcTest(AssetAllocationController.class)
@Import(GlobalExceptionHandler.class)
class AssetAllocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetAllocationSyncService service;

    private UUID userId;
    private AssetAllocationResponseDTO testDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testDTO = new AssetAllocationResponseDTO();
        testDTO.setId(UUID.randomUUID().toString());
        testDTO.setUserId(userId.toString());
        testDTO.setAccountId(UUID.randomUUID().toString());
        testDTO.setAccountDisplayName("Stocks Account");
        testDTO.setAccountType("BROKERAGE");
        testDTO.setInstrumentType("BROKERAGE");
        testDTO.setCurrentValue("6000.00");
        testDTO.setWeightPct("60.0000");
        testDTO.setCurrency("EUR");
    }

    // GET /api/analytics/asset-allocation

    @Test
    void getAll_returns200WithList() throws Exception {
        when(service.getAll(userId)).thenReturn(List.of(testDTO));

        mockMvc.perform(get("/api/analytics/asset-allocation")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountDisplayName").value("Stocks Account"))
                .andExpect(jsonPath("$[0].weightPct").value("60.0000"));
    }

    @Test
    void getAll_returns200WithEmptyList_whenNoAllocations() throws Exception {
        when(service.getAll(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/asset-allocation")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // POST /api/analytics/asset-allocation/sync

    @Test
    void sync_returns204() throws Exception {
        doNothing().when(service).sync(userId);

        mockMvc.perform(post("/api/analytics/asset-allocation/sync")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());
    }
}