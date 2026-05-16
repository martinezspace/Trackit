package com.trackit.bankaccountservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.bankaccountservice.dto.TransactionCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionUpdateDTO;
import com.trackit.bankaccountservice.exception.GlobalExceptionHandler;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.model.TransactionDirection;
import com.trackit.bankaccountservice.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    private UUID userId;
    private UUID transactionId;
    private TransactionResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        testResponseDTO = new TransactionResponseDTO();
        testResponseDTO.setId(transactionId.toString());
        testResponseDTO.setAmount("99.99");
        testResponseDTO.setDirection("OUTBOUND");
        testResponseDTO.setCurrency("PLN");
    }

    // GET /api/transactions/{id}

    @Test
    public void getTransaction_returns200_whenFound() throws Exception {
        when(transactionService.getTransactionById(transactionId, userId))
                .thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/transactions/{id}", transactionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value("99.99"))
                .andExpect(jsonPath("$.direction").value("OUTBOUND"));
    }

    @Test
    public void getTransaction_returns404_whenNotFound() throws Exception {
        when(transactionService.getTransactionById(transactionId, userId))
                .thenThrow(new ResourceNotFoundException("Transaction not found: " + transactionId));

        mockMvc.perform(get("/api/transactions/{id}", transactionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // POST /api/transactions

    @Test
    public void createTransaction_returns201_withCreatedDTO() throws Exception {
        TransactionCreateDTO createDTO = new TransactionCreateDTO();
        createDTO.setAccountId(UUID.randomUUID());
        createDTO.setAmount(new BigDecimal("99.99"));
        createDTO.setDirection(TransactionDirection.OUTBOUND);
        createDTO.setCurrency("PLN");
        createDTO.setBookingDate(LocalDate.now());

        when(transactionService.createTransaction(eq(userId), any(TransactionCreateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(post("/api/transactions")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value("99.99"))
                .andExpect(jsonPath("$.direction").value("OUTBOUND"));
    }

    @Test
    public void createTransaction_returns422_whenAmountMissing() throws Exception {
        TransactionCreateDTO createDTO = new TransactionCreateDTO();
        createDTO.setAccountId(UUID.randomUUID());
        createDTO.setDirection(TransactionDirection.OUTBOUND);
        createDTO.setCurrency("PLN");
        createDTO.setBookingDate(LocalDate.now());
        // amount intentionally missing - should fail @NotNull validation

        mockMvc.perform(post("/api/transactions")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    public void createTransaction_returns422_whenAccountIdMissing() throws Exception {
        TransactionCreateDTO createDTO = new TransactionCreateDTO();
        createDTO.setAmount(new BigDecimal("99.99"));
        createDTO.setDirection(TransactionDirection.OUTBOUND);
        createDTO.setCurrency("PLN");
        createDTO.setBookingDate(LocalDate.now());
        // accountId intentionally missing

        mockMvc.perform(post("/api/transactions")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.accountId").exists());
    }

    // PATCH /api/transactions/{id}

    @Test
    public void updateTransaction_returns200_whenValid() throws Exception {
        TransactionUpdateDTO updateDTO = new TransactionUpdateDTO();
        updateDTO.setNotes("Grocery run");

        when(transactionService.updateTransaction(eq(transactionId), eq(userId), any(TransactionUpdateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(patch("/api/transactions/{id}", transactionId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    // DELETE /api/transactions/{id}

    @Test
    public void deleteTransaction_returns204_whenManual() throws Exception {
        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        verify(transactionService, times(1)).deleteTransaction(transactionId, userId);
    }

    @Test
    public void deleteTransaction_returns400_whenTransactionIsNotManual() throws Exception {
        doThrow(new IllegalArgumentException("Only manually created transactions can be deleted"))
                .when(transactionService).deleteTransaction(any(), any());

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isBadRequest());
    }
}