package com.trackit.investmentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.investmentservice.dto.InstrumentCreateDTO;
import com.trackit.investmentservice.dto.InstrumentResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.model.InstrumentType;
import com.trackit.investmentservice.service.InstrumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InstrumentController.class)
@TestPropertySource(properties = "internal.key=internal-service-key")
public class InstrumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InstrumentService instrumentService;

    private UUID instrumentId;
    private InstrumentResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        instrumentId = UUID.randomUUID();

        testResponseDTO = new InstrumentResponseDTO();
        testResponseDTO.setId(instrumentId.toString());
        testResponseDTO.setIsin("PL0009999944");
        testResponseDTO.setTicker("CDR");
        testResponseDTO.setName("CD Projekt SA");
        testResponseDTO.setInstrumentType("STOCK");
        testResponseDTO.setCurrency("PLN");
    }

    //GET /api/instruments/{id}
    @Test
    void getById_returns200WithDTO() throws Exception {
        when(instrumentService.getById(instrumentId))
                .thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/instruments/{id}", instrumentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isin").value("PL0009999944"))
                .andExpect(jsonPath("$.ticker").value("CDR"));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(instrumentService.getById(instrumentId))
                .thenThrow(new ResourceNotFoundException("Instrument not found"));

        mockMvc.perform(get("/api/instruments/{id}", instrumentId))
                .andExpect(status().isNotFound());
    }

    //GET /api/instruments/isin/{isin}
    @Test
    void getByIsin_returns200WithDTO() throws Exception {
        when(instrumentService.getByIsin("PL0009999944"))
                .thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/instruments/isin/PL0009999944"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("CDR"))
                .andExpect(jsonPath("$.name").value("CD Projekt SA"));
    }

    @Test
    void getByIsin_returns404_whenNotFound() throws Exception {
        when(instrumentService.getByIsin("PL0009999944"))
                .thenThrow(new ResourceNotFoundException("Instrument not found for ISIN"));

        mockMvc.perform(get("/api/instruments/isin/PL0009999944"))
                .andExpect(status().isNotFound());
    }

    //POST /api/instruments
    @Test
    void createInstrument_returns201_withValidKeyAndBody() throws Exception {
        InstrumentCreateDTO createDTO = new InstrumentCreateDTO();
        createDTO.setIsin("PL0009999944");
        createDTO.setName("CD Projekt SA");
        createDTO.setInstrumentType(InstrumentType.STOCK);
        createDTO.setCurrency("PLN");

        when(instrumentService.createInstrument(any(InstrumentCreateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(post("/api/instruments")
                        .header("X-Internal-Key", "internal-service-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isin").value("PL0009999944"));
    }

    @Test
    void createInstrument_returns403_withInvalidKey() throws Exception {
        InstrumentCreateDTO createDTO = new InstrumentCreateDTO();
        createDTO.setIsin("PL0009999944");
        createDTO.setName("CD Projekt SA");
        createDTO.setInstrumentType(InstrumentType.STOCK);
        createDTO.setCurrency("PLN");

        mockMvc.perform(post("/api/instruments")
                        .header("X-Internal-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createInstrument_returns400_whenIsinMissing() throws Exception {
        InstrumentCreateDTO createDTO = new InstrumentCreateDTO();
        // isin intentionally missing — should fail @NotBlank validation
        createDTO.setName("CD Projekt SA");
        createDTO.setInstrumentType(InstrumentType.STOCK);
        createDTO.setCurrency("PLN");

        mockMvc.perform(post("/api/instruments")
                        .header("X-Internal-Key", "internal-service-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createInstrument_returns400_whenCurrencyInvalidFormat() throws Exception {
        InstrumentCreateDTO createDTO = new InstrumentCreateDTO();
        createDTO.setIsin("PL0009999944");
        createDTO.setName("CD Projekt SA");
        createDTO.setInstrumentType(InstrumentType.STOCK);
        createDTO.setCurrency("pl"); // lowercase — fails @Pattern [A-Z]{3}

        mockMvc.perform(post("/api/instruments")
                        .header("X-Internal-Key", "internal-service-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }
}
