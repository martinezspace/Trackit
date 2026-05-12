package com.trackit.bankaccountservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.bankaccountservice.dto.TransactionRuleCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleUpdateDTO;
import com.trackit.bankaccountservice.exception.GlobalExceptionHandler;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.model.RuleMatchField;
import com.trackit.bankaccountservice.service.TransactionRuleService;
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

@WebMvcTest(TransactionRuleController.class)
@Import(GlobalExceptionHandler.class)
public class TransactionRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionRuleService transactionRuleService;

    private UUID userId;
    private UUID ruleId;
    private TransactionRuleResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ruleId = UUID.randomUUID();

        testResponseDTO = new TransactionRuleResponseDTO();
        testResponseDTO.setId(ruleId.toString());
        testResponseDTO.setName("Lidl rule");
        testResponseDTO.setMatchPattern("lidl");
        testResponseDTO.setPriority(100);
        testResponseDTO.setActive(true);
    }

    // GET /api/transaction-rules

    @Test
    public void getAllRules_returns200_withList() throws Exception {
        when(transactionRuleService.getAllRulesForUser(userId)).thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/transaction-rules")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lidl rule"))
                .andExpect(jsonPath("$[0].priority").value(100));
    }

    @Test
    public void getAllRules_returns200_withEmptyList_whenUserHasNoRules() throws Exception {
        when(transactionRuleService.getAllRulesForUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/transaction-rules")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // GET /api/transaction-rules/{id}

    @Test
    public void getRule_returns200_whenFound() throws Exception {
        when(transactionRuleService.getRuleById(ruleId, userId)).thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/transaction-rules/{id}", ruleId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lidl rule"));
    }

    @Test
    public void getRule_returns404_whenNotFound() throws Exception {
        when(transactionRuleService.getRuleById(ruleId, userId))
                .thenThrow(new ResourceNotFoundException("Rule not found: " + ruleId));

        mockMvc.perform(get("/api/transaction-rules/{id}", ruleId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // POST /api/transaction-rules

    @Test
    public void createRule_returns201_withCreatedDTO() throws Exception {
        TransactionRuleCreateDTO createDTO = new TransactionRuleCreateDTO();
        createDTO.setName("Lidl rule");
        createDTO.setMatchField(RuleMatchField.MERCHANT);
        createDTO.setMatchPattern("lidl");
        createDTO.setCategoryId(UUID.randomUUID());

        when(transactionRuleService.createRule(eq(userId), any(TransactionRuleCreateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(post("/api/transaction-rules")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Lidl rule"));
    }

    @Test
    public void createRule_returns422_whenNameMissing() throws Exception {
        TransactionRuleCreateDTO createDTO = new TransactionRuleCreateDTO();
        createDTO.setMatchField(RuleMatchField.MERCHANT);
        createDTO.setMatchPattern("lidl");
        createDTO.setCategoryId(UUID.randomUUID());
        // name intentionally missing

        mockMvc.perform(post("/api/transaction-rules")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    public void createRule_returns422_whenCategoryIdMissing() throws Exception {
        TransactionRuleCreateDTO createDTO = new TransactionRuleCreateDTO();
        createDTO.setName("Lidl rule");
        createDTO.setMatchField(RuleMatchField.MERCHANT);
        createDTO.setMatchPattern("lidl");
        // categoryId intentionally missing

        mockMvc.perform(post("/api/transaction-rules")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }

    // DELETE /api/transaction-rules/{id}

    @Test
    public void deleteRule_returns204_whenSuccessful() throws Exception {
        mockMvc.perform(delete("/api/transaction-rules/{id}", ruleId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        verify(transactionRuleService, times(1)).deleteRule(ruleId, userId);
    }

    @Test
    public void deleteRule_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Rule not found"))
                .when(transactionRuleService).deleteRule(any(), any());

        mockMvc.perform(delete("/api/transaction-rules/{id}", ruleId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }
}