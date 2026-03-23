package com.trackit.investmentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.investmentservice.dto.ImportBatchCreateDTO;
import com.trackit.investmentservice.dto.ImportBatchResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.model.BrokerFormat;
import com.trackit.investmentservice.service.ImportBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImportBatchController.class)
class ImportBatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ImportBatchService importBatchService;

    private UUID userId;
    private UUID accountId;
    private UUID batchId;
    private ImportBatchResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        batchId = UUID.randomUUID();

        testResponseDTO = new ImportBatchResponseDTO();
        testResponseDTO.setId(batchId.toString());
        testResponseDTO.setAccountId(accountId.toString());
        testResponseDTO.setBrokerFormat("TRADING212");
        testResponseDTO.setFilename("from_2024-01-01_to_2024-12-31.csv");
        testResponseDTO.setStatus("PENDING");
    }

    // GET /api/import-batches?accountId={accountId}

    @Test
    void getAllBatches_returns200WithList() throws Exception {
        when(importBatchService.getAllBatchesForAccount(accountId, userId))
                .thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/import-batches")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].brokerFormat").value("TRADING212"));
    }

    @Test
    void getAllBatches_returns404_whenAccountNotFound() throws Exception {
        when(importBatchService.getAllBatchesForAccount(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/api/import-batches")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // GET /api/import-batches/{id}?accountId={accountId}

    @Test
    void getBatchById_returns200WithDTO() throws Exception {
        when(importBatchService.getBatchById(batchId, accountId, userId))
                .thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/import-batches/{id}", batchId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.filename").value("from_2024-01-01_to_2024-12-31.csv"));
    }

    @Test
    void getBatchById_returns404_whenBatchNotFound() throws Exception {
        when(importBatchService.getBatchById(batchId, accountId, userId))
                .thenThrow(new ResourceNotFoundException("Import batch not found"));

        mockMvc.perform(get("/api/import-batches/{id}", batchId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // POST /api/import-batches

    @Test
    void createBatch_returns201WithDTO() throws Exception {
        ImportBatchCreateDTO createDTO = new ImportBatchCreateDTO();
        createDTO.setAccountId(accountId);
        createDTO.setBrokerFormat(BrokerFormat.TRADING212);
        createDTO.setFilename("from_2024-01-01_to_2024-12-31.csv");

        when(importBatchService.createBatch(any(ImportBatchCreateDTO.class), eq(userId)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(post("/api/import-batches")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.brokerFormat").value("TRADING212"));
    }

    @Test
    void createBatch_returns400_whenAccountIdMissing() throws Exception {
        ImportBatchCreateDTO createDTO = new ImportBatchCreateDTO();
        // accountId intentionally missing
        createDTO.setBrokerFormat(BrokerFormat.TRADING212);
        createDTO.setFilename("test.csv");

        mockMvc.perform(post("/api/import-batches")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBatch_returns400_whenBrokerFormatMissing() throws Exception {
        ImportBatchCreateDTO createDTO = new ImportBatchCreateDTO();
        createDTO.setAccountId(accountId);
        // brokerFormat intentionally missing
        createDTO.setFilename("test.csv");

        mockMvc.perform(post("/api/import-batches")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    // PATCH /api/import-batches/{id}/cancel

    @Test
    void cancelBatch_returns200WithCancelledStatus() throws Exception {
        ImportBatchResponseDTO cancelledResponse = new ImportBatchResponseDTO();
        cancelledResponse.setId(batchId.toString());
        cancelledResponse.setStatus("CANCELLED");

        when(importBatchService.cancelBatch(batchId, accountId, userId))
                .thenReturn(cancelledResponse);

        mockMvc.perform(patch("/api/import-batches/{id}/cancel", batchId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelBatch_returns404_whenBatchNotFound() throws Exception {
        when(importBatchService.cancelBatch(batchId, accountId, userId))
                .thenThrow(new ResourceNotFoundException("Import batch not found"));

        mockMvc.perform(patch("/api/import-batches/{id}/cancel", batchId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }
}