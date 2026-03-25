package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.HoldingResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.service.HoldingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HoldingController.class)
class HoldingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HoldingService holdingService;

    private UUID userId;
    private UUID accountId;
    private UUID instrumentId;
    private HoldingResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        instrumentId = UUID.randomUUID();

        testResponseDTO = new HoldingResponseDTO();
        testResponseDTO.setAccountId(accountId.toString());
        testResponseDTO.setInstrumentTicker("CSPX");
        testResponseDTO.setInstrumentName("iShares Core S&P 500");
        testResponseDTO.setQuantity("1.150000");
        testResponseDTO.setAvgPurchasePrice("516.7500");
        testResponseDTO.setTotalInvested("2399.99");
        testResponseDTO.setCurrency("PLN");
    }

    // GET /api/holdings?accountId={accountId}
    @Test
    void getAllHoldings_returns200WithList() throws Exception {
        when(holdingService.getAllHoldingsForAccount(accountId, userId))
                .thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/holdings")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instrumentTicker").value("CSPX"))
                .andExpect(jsonPath("$[0].quantity").value("1.150000"))
                .andExpect(jsonPath("$[0].totalInvested").value("2399.99"));
    }

    @Test
    void getAllHoldings_returnsEmptyList_whenNoHoldings() throws Exception {
        when(holdingService.getAllHoldingsForAccount(accountId, userId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/holdings")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllHoldings_returns404_whenAccountNotFound() throws Exception {
        when(holdingService.getAllHoldingsForAccount(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/api/holdings")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // GET /api/holdings/instrument/{instrumentId}?accountId={accountId}
    @Test
    void getHoldingByInstrument_returns200WithDTO() throws Exception {
        when(holdingService.getHoldingByInstrument(accountId, instrumentId, userId))
                .thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/holdings/instrument/{instrumentId}", instrumentId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instrumentTicker").value("CSPX"))
                .andExpect(jsonPath("$.avgPurchasePrice").value("516.7500"));
    }

    @Test
    void getHoldingByInstrument_returns404_whenNotFound() throws Exception {
        when(holdingService.getHoldingByInstrument(accountId, instrumentId, userId))
                .thenThrow(new ResourceNotFoundException("Holding not found for instrument"));

        mockMvc.perform(get("/api/holdings/instrument/{instrumentId}", instrumentId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }
}