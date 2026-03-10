package com.trackit.investmentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.investmentservice.dto.InvestmentAccountCreateDTO;
import com.trackit.investmentservice.dto.InvestmentAccountResponseDTO;
import com.trackit.investmentservice.dto.InvestmentAccountUpdateDTO;
import com.trackit.investmentservice.model.AccountType;
import com.trackit.investmentservice.service.InvestmentAccountService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest loads only the web layer - controller, filters, validation
// Service is mocked - no real DB or business logic runs
@WebMvcTest(InvestmentAccountController.class)
public class InvestmentAccountControllerTest {

    @Autowired
    private MockMvc mockMVc;

    @Autowired
    private ObjectMapper objectMapper;

    // MockitoBean replaces the real service with a Mockito mock in the Spring contexxt
    @MockitoBean
    private InvestmentAccountService investmentAccountService;

    private UUID userId;
    private UUID accountId;
    private InvestmentAccountResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testResponseDTO = new InvestmentAccountResponseDTO();
        testResponseDTO.setId(accountId.toString());
        testResponseDTO.setAccountType("IKE");
        testResponseDTO.setBrokerName("XTB");
        testResponseDTO.setDisplayName("Moje IKE");
        testResponseDTO.setCurrency("PLN");
        testResponseDTO.setActive(true);
    }

    // GET /api/investment-accounts

    @Test
    void getAllAccounts_returns200WithList() throws Exception {
        when(investmentAccountService.getAllAccountsForUser(userId))
                .thenReturn(List.of(testResponseDTO));

        mockMVc.perform(get("/api/investment-accounts")
                    .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brokerName").value("XTB"))
                .andExpect(jsonPath("$[0].accountType").value("IKE"));
    }

    @Test
    void getAllAccounts_returnsEmptyList_whenUserHasNoAccounts() throws Exception {
        when(investmentAccountService.getAllAccountsForUser(userId))
                .thenReturn(List.of());

        mockMVc.perform(get("/api/investment-accounts")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())    // verify it's an array
                .andExpect(jsonPath("$").isEmpty());   // verify it's empty
    }

    // GET /api/investment-accounts/{id}

    @Test
    void getAccountById_returns200WithDTO() throws Exception {
        when(investmentAccountService.getAccountById(accountId, userId))
                .thenReturn(testResponseDTO);

        mockMVc.perform(get("/api/investment-accounts/{id}", accountId)
                    .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brokerName").value("XTB"))
                .andExpect(jsonPath("$.accountType").value("IKE"));
    }

    @Test
    void getAccountById_returns500_whenAccountNotFound() throws Exception {
        when(investmentAccountService.getAccountById(accountId, userId))
                .thenThrow(new RuntimeException("Account not found"));

        mockMVc.perform(get("/api/investment-accounts/{id}", accountId)
                    .header("X-User-Id", userId.toString()))
                .andExpect(status().isInternalServerError());
    }

    // POST /api/investment-accounts

    @Test
    void createAccount_returns200WithDTO() throws Exception {
        InvestmentAccountCreateDTO createDTO = new InvestmentAccountCreateDTO();
        createDTO.setAccountType(AccountType.IKE);
        createDTO.setBrokerName("XTB");
        createDTO.setCurrency("PLN");

        when(investmentAccountService.createAccount(eq(userId), any(InvestmentAccountCreateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMVc.perform(post("/api/investment-accounts")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.brokerName").value("XTB"))
                .andExpect(jsonPath("$.accountType").value("IKE"));
    }

    @Test
    void createAccount_returns400_whenBrokerNameMissing() throws Exception {
        InvestmentAccountCreateDTO createDTO = new InvestmentAccountCreateDTO();
        createDTO.setAccountType(AccountType.IKE);
        // brokerName intentionally missing — should fail @NotBlank validation

        mockMVc.perform(post("/api/investment-accounts")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_returns400_whenAccountTypeMissing() throws Exception {
        InvestmentAccountCreateDTO createDTO = new InvestmentAccountCreateDTO();
        createDTO.setBrokerName("XTB");
        // accountType intentionally missing — should fail @NotNull validation

        mockMVc.perform(post("/api/investment-accounts")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    // PATCH /api/investment-accounts/{id}

    @Test
    void updateAccount_returns200WithUpdatedDTO() throws Exception {
        InvestmentAccountUpdateDTO updateDTO = new InvestmentAccountUpdateDTO();
        updateDTO.setDisplayName("Updated IKE");

        InvestmentAccountResponseDTO updatedResponse = new InvestmentAccountResponseDTO();
        updatedResponse.setId(accountId.toString());
        updatedResponse.setDisplayName("Updated IKE");
        updatedResponse.setBrokerName("XTB");

        when(investmentAccountService.updateAccount(eq(accountId), eq(userId), any(InvestmentAccountUpdateDTO.class)))
                .thenReturn(updatedResponse);

        mockMVc.perform(patch("/api/investment-accounts/{id}", accountId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated IKE"));
    }

    // DELETE /api/investment-accounts/{id}

    @Test
    void deactivateAccount_returns204() throws Exception {
        mockMVc.perform(delete("/api/investment-accounts/{id}", accountId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        verify(investmentAccountService, times(1)).deactiveAccount(accountId, userId);
    }
}
