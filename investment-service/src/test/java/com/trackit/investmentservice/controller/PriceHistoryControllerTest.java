package com.trackit.investmentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.investmentservice.dto.PriceHistoryCreateDTO;
import com.trackit.investmentservice.dto.PriceHistoryResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.service.PriceHistoryService;
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

@WebMvcTest(PriceHistoryController.class)
@TestPropertySource(properties = "internal.key=internal-service-key")
class PriceHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PriceHistoryService priceHistoryService;

    private UUID instrumentId;
    private PriceHistoryResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        instrumentId = UUID.randomUUID();

        testResponseDTO = new PriceHistoryResponseDTO();
        testResponseDTO.setInstrumentId(instrumentId.toString());
        testResponseDTO.setInstrumentTicker("CSPX");
        testResponseDTO.setPriceDate("2024-03-15");
        testResponseDTO.setClosePrice("520.7500");
        testResponseDTO.setCurrency("USD");
        testResponseDTO.setSource("ALPHA_VANTAGE");
    }

    // GET /api/price-history/{instrumentId}/latest
    @Test
    void getLatestPrice_returns200WithDTO() throws Exception {
        when(priceHistoryService.getLatestPrice(instrumentId))
                .thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/price-history/{instrumentId}/latest", instrumentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closePrice").value("520.7500"))
                .andExpect(jsonPath("$.instrumentTicker").value("CSPX"));
    }

    @Test
    void getLatestPrice_returns404_whenNoPriceExists() throws Exception {
        when(priceHistoryService.getLatestPrice(instrumentId))
                .thenThrow(new ResourceNotFoundException("No price history found for instrument"));

        mockMvc.perform(get("/api/price-history/{instrumentId}/latest", instrumentId))
                .andExpect(status().isNotFound());
    }

    // GET /api/price-history/{instrumentId}?from=&to=
    @Test
    void getPriceHistory_returns200WithList() throws Exception {
        when(priceHistoryService.getPriceHistory(
                eq(instrumentId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/price-history/{instrumentId}", instrumentId)
                        .param("from", "2024-01-01")
                        .param("to", "2024-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priceDate").value("2024-03-15"))
                .andExpect(jsonPath("$[0].closePrice").value("520.7500"));
    }

    // POST /api/price-history
    @Test
    void savePrice_returns201_withValidKey() throws Exception {
        PriceHistoryCreateDTO createDTO = new PriceHistoryCreateDTO();
        createDTO.setInstrumentId(instrumentId);
        createDTO.setPriceDate(LocalDate.of(2024, 3, 15));
        createDTO.setClosePrice(new BigDecimal("520.7500"));
        createDTO.setCurrency("USD");
        createDTO.setSource("ALPHA_VANTAGE");

        when(priceHistoryService.savePrice(any(PriceHistoryCreateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(post("/api/price-history")
                        .header("X-Internal-Key", "internal-service-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.closePrice").value("520.7500"));
    }

    @Test
    void savePrice_returns403_withInvalidKey() throws Exception {
        PriceHistoryCreateDTO createDTO = new PriceHistoryCreateDTO();
        createDTO.setInstrumentId(instrumentId);
        createDTO.setPriceDate(LocalDate.of(2024, 3, 15));
        createDTO.setClosePrice(new BigDecimal("520.7500"));
        createDTO.setCurrency("USD");

        mockMvc.perform(post("/api/price-history")
                        .header("X-Internal-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void savePrice_returns400_whenClosePriceMissing() throws Exception {
        PriceHistoryCreateDTO createDTO = new PriceHistoryCreateDTO();
        createDTO.setInstrumentId(instrumentId);
        createDTO.setPriceDate(LocalDate.of(2024, 3, 15));
        // closePrice intentionally missing
        createDTO.setCurrency("USD");

        mockMvc.perform(post("/api/price-history")
                        .header("X-Internal-Key", "internal-service-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }
}