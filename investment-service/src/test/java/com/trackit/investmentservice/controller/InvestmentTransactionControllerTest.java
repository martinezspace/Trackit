package com.trackit.investmentservice.controller;

import com.trackit.investmentservice.dto.InvestmentTransactionResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.service.InvestmentTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvestmentTransactionController.class)
class InvestmentTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvestmentTransactionService transactionService;

    private UUID userId;
    private UUID accountId;
    private UUID transactionId;
    private UUID instrumentId;
    private InvestmentTransactionResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
        instrumentId = UUID.randomUUID();

        testResponseDTO = new InvestmentTransactionResponseDTO();
        testResponseDTO.setId(transactionId.toString());
        testResponseDTO.setAccountId(accountId.toString());
        testResponseDTO.setTransactionType("BUY");
        testResponseDTO.setInstrumentName("iShares Core S&P 500");
        testResponseDTO.setInstrumentTicker("CSPX");
        testResponseDTO.setQuantity("1.150000");
        testResponseDTO.setPrice("516.7500");
        testResponseDTO.setAmount("2399.99");
        testResponseDTO.setCurrency("PLN");
        testResponseDTO.setTransactionDate("2024-01-30");
        testResponseDTO.setCancelled(false);
    }

    // GET /api/transactions?accountId={accountId}
    @Test
    void getAllTransactions_returns200WithList() throws Exception {
        when(transactionService.getAllTransactionsForAccount(accountId, userId))
                .thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionType").value("BUY"))
                .andExpect(jsonPath("$[0].instrumentTicker").value("CSPX"))
                .andExpect(jsonPath("$[0].amount").value("2399.99"));
    }

    @Test
    void getAllTransactions_returns404_whenAccountNotFound() throws Exception {
        when(transactionService.getAllTransactionsForAccount(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account Not Found"));

        mockMvc.perform(get("/api/transactions")
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // GET /api/transactions/instrument/{instrumentId}?accountId={accountId}
    @Test
    void getTransactionsByInstrument_returns200WithList() throws Exception {
        when(transactionService.getTransactionsByInstrument(accountId, instrumentId, userId))
                .thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/transactions/instrument/{instrumentId}", instrumentId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionType").value("BUY"));
    }

    // GET /api/transactions/{id}?accountId={accountId}
    @Test
    void getTransactionById_returns200WithDTO() throws Exception {
        when(transactionService.getTransactionById(transactionId, accountId, userId))
                .thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/transactions/{id}", transactionId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("BUY"))
                .andExpect(jsonPath("$.instrumentName").value("iShares Core S&P 500"));
    }

    @Test
    void getTransactionById_returns404_whenNotFound() throws Exception {
        when(transactionService.getTransactionById(transactionId, accountId, userId))
                .thenThrow(new ResourceNotFoundException("Transaction Not Found"));

        mockMvc.perform(get("/api/transactions/{id}", transactionId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // PATCH /api/transactions/{id}/cancel?accountId={accountId}
    @Test
    void cancelTransaction_returns200WithCancelledDTO() throws Exception {
        InvestmentTransactionResponseDTO cancelledResponse = new InvestmentTransactionResponseDTO();
        cancelledResponse.setId(transactionId.toString());
        cancelledResponse.setCancelled(true);

        when(transactionService.cancelTransaction(transactionId, accountId, userId))
                .thenReturn(cancelledResponse);

        mockMvc.perform(patch("/api/transactions/{id}/cancel", transactionId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(true));
    }

    @Test
    void cancelTransaction_returns404_whenTransactionNotFound() throws Exception {
        when(transactionService.cancelTransaction(transactionId, accountId, userId))
                .thenThrow(new ResourceNotFoundException("Transaction Not Found"));

        mockMvc.perform(patch("/api/transactions/{id}/cancel", transactionId)
                        .param("accountId", accountId.toString())
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }
}