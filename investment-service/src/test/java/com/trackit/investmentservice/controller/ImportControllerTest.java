package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.ImportBatchResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.service.ImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportController.class)
@TestPropertySource(properties = "internal.key=internal-service-key")
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportService importService;

    private UUID userId;
    private UUID accountId;
    private ImportBatchResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testResponseDTO = new ImportBatchResponseDTO();
        testResponseDTO.setId(UUID.randomUUID().toString());
        testResponseDTO.setStatus("PENDING");
    }

    // POST /api/import
    @Test
    void importCsv_returns202_whenValidCsvUploaded() throws Exception {
        when(importService.initiateImport(any(), any(), any(), any()))
                .thenReturn(testResponseDTO);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                "Action,Time\nMarket buy,2024-01-01".getBytes());

        mockMvc.perform(multipart("/api/import")
                        .file(file)
                        .param("accountId", accountId.toString())
                        .param("brokerFormat", "TRADING212_STANDARD")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void importCsv_returns202_whenValidXlsUploaded() throws Exception {
        when(importService.initiateImport(any(), any(), any(), any()))
                .thenReturn(testResponseDTO);

        MockMultipartFile file = new MockMultipartFile(
                "file", "ppk.xls",
                "application/vnd.ms-excel",
                new byte[]{0x50, 0x4B}); // dummy bytes

        mockMvc.perform(multipart("/api/import")
                        .file(file)
                        .param("accountId", accountId.toString())
                        .param("brokerFormat", "NATIONALE_NEDERLANDEN_PPK")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isAccepted());
    }

    @Test
    void importCsv_returns400_whenFileIsEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/import")
                        .file(emptyFile)
                        .param("accountId", accountId.toString())
                        .param("brokerFormat", "TRADING212_STANDARD")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importCsv_returns400_whenUnsupportedFileType() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf",
                "PDF content".getBytes());

        mockMvc.perform(multipart("/api/import")
                        .file(pdfFile)
                        .param("accountId", accountId.toString())
                        .param("brokerFormat", "TRADING212_STANDARD")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importCsv_returns400_whenAccountNotFound() throws Exception {
        when(importService.initiateImport(any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Account not found"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                "Action,Time\nMarket buy,2024-01-01".getBytes());

        mockMvc.perform(multipart("/api/import")
                        .file(file)
                        .param("accountId", accountId.toString())
                        .param("brokerFormat", "TRADING212_STANDARD")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void importCsv_returns400_whenImportAlreadyInProgress() throws Exception {
        when(importService.initiateImport(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException(
                        "An import is already in progress for this account"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                "Action,Time\nMarket buy,2024-01-01".getBytes());

        mockMvc.perform(multipart("/api/import")
                        .file(file)
                        .param("accountId", accountId.toString())
                        .param("brokerFormat", "TRADING212_STANDARD")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isBadRequest());
    }
}