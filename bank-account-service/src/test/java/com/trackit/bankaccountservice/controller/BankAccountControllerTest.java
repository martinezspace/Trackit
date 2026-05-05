package com.trackit.bankaccountservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.bankaccountservice.dto.BankAccountCreateDTO;
import com.trackit.bankaccountservice.dto.BankAccountResponseDTO;
import com.trackit.bankaccountservice.dto.BankAccountUpdateDTO;
import com.trackit.bankaccountservice.exception.GlobalExceptionHandler;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.service.BankAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankAccountController.class)
@Import(GlobalExceptionHandler.class)
public class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BankAccountService bankAccountService;

    private UUID userId;
    private UUID accountId;
    private BankAccountResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testResponseDTO = new BankAccountResponseDTO();
        testResponseDTO.setId(accountId.toString());
        testResponseDTO.setName("Current Account");
        testResponseDTO.setCurrency("PLN");
        testResponseDTO.setActive(true);
    }

    // GET /api/bank-accounts

    @Test
    public void getAllAccounts_returns200_withList() throws Exception {
        when(bankAccountService.getAllAccountsForUser(userId)).thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/bank-accounts")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Current Account"))
                .andExpect(jsonPath("$[0].currency").value("PLN"));
    }

    @Test
    public void getAllAccounts_returns200_withEmptyList_whenUserHasNoAccounts() throws Exception {
        when(bankAccountService.getAllAccountsForUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/bank-accounts")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // GET /api/bank-accounts/{id}

    @Test
    public void getAccount_returns200_whenFound() throws Exception {
        when(bankAccountService.getAccountById(accountId, userId)).thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/bank-accounts/{id}", accountId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Current Account"));
    }

    @Test
    public void getAccount_returns404_whenNotFound() throws Exception {
        when(bankAccountService.getAccountById(accountId, userId))
                .thenThrow(new ResourceNotFoundException("Account not found: " + accountId));

        mockMvc.perform(get("/api/bank-accounts/{id}", accountId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // POST /api/bank-accounts

    @Test
    public void createAccount_returns201_withCreatedDTO() throws Exception {
        BankAccountCreateDTO createDTO = new BankAccountCreateDTO();
        createDTO.setConnectionId(UUID.randomUUID());
        createDTO.setExternalAccountId("ext-001");
        createDTO.setName("Current Account");

        when(bankAccountService.createAccount(eq(userId), any(BankAccountCreateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(post("/api/bank-accounts")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Current Account"));
    }

    @Test
    public void createAccount_returns422_whenConnectionIdMissing() throws Exception {
        BankAccountCreateDTO createDTO = new BankAccountCreateDTO();
        createDTO.setExternalAccountId("ext-001");
        createDTO.setName("Current Account");
        // connectionId intentionally missing - should fail @NotNull validation

        mockMvc.perform(post("/api/bank-accounts")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.connectionId").exists());
    }

    @Test
    public void createAccount_returns422_whenNameMissing() throws Exception {
        BankAccountCreateDTO createDTO = new BankAccountCreateDTO();
        createDTO.setConnectionId(UUID.randomUUID());
        createDTO.setExternalAccountId("ext-001");
        // name intentionally missing

        mockMvc.perform(post("/api/bank-accounts")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    // PATCH /api/bank-accounts/{id}

    @Test
    public void updateAccount_returns200_withUpdatedDTO() throws Exception {
        BankAccountUpdateDTO updateDTO = new BankAccountUpdateDTO();
        updateDTO.setDisplayName("My Main Account");

        BankAccountResponseDTO updatedResponse = new BankAccountResponseDTO();
        updatedResponse.setDisplayName("My Main Account");

        when(bankAccountService.updateAccount(eq(accountId), eq(userId), any(BankAccountUpdateDTO.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/bank-accounts/{id}", accountId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("My Main Account"));
    }

    // DELETE /api/bank-accounts/{id}

    @Test
    public void deactivateAccount_returns204_whenSuccessful() throws Exception {
        mockMvc.perform(delete("/api/bank-accounts/{id}", accountId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        verify(bankAccountService, times(1)).deactivateAccount(accountId, userId);
    }

    @Test
    public void deactivateAccount_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Account not found"))
                .when(bankAccountService).deactivateAccount(any(), any());

        mockMvc.perform(delete("/api/bank-accounts/{id}", accountId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }
}