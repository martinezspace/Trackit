package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.TransactionRuleCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionRuleUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.TransactionRuleMapper;
import com.trackit.bankaccountservice.model.*;
import com.trackit.bankaccountservice.repository.CategoryRepository;
import com.trackit.bankaccountservice.repository.TransactionRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class TransactionRuleServiceTest {

    @Mock
    private TransactionRuleRepository transactionRuleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRuleMapper transactionRuleMapper;

    @InjectMocks
    private TransactionRuleService transactionRuleService;

    private UUID userId;
    private UUID ruleId;
    private UUID categoryId;
    private Category foodCategory;
    private TransactionRule testRule;
    private TransactionRuleResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        // Build reusable food category
        foodCategory = new Category();
        foodCategory.setName("Food & Dining");
        foodCategory.setType(CategoryType.DEBIT);
        foodCategory.setSystem(true);

        // Build reusable test rule
        testRule = new TransactionRule();
        testRule.setUserId(userId);
        testRule.setName("Lidl rule");
        testRule.setMatchField(RuleMatchField.MERCHANT);
        testRule.setMatchPattern("lidl");
        testRule.setCategory(foodCategory);
        testRule.setPriority(100);
        testRule.setActive(true);

        // Build reusable test response DTO
        testResponseDTO = new TransactionRuleResponseDTO();
        testResponseDTO.setId(ruleId.toString());
        testResponseDTO.setName("Lidl rule");
        testResponseDTO.setMatchPattern("lidl");
        testResponseDTO.setPriority(100);
        testResponseDTO.setActive(true);
    }

    // getAllRulesForUser

    @Test
    public void getAllRulesForUser_returnsListOfDTOs() {
        when(transactionRuleRepository.findByUserIdAndActiveTrueOrderByPriorityDesc(userId))
                .thenReturn(List.of(testRule));
        when(transactionRuleMapper.toResponseDTO(testRule)).thenReturn(testResponseDTO);

        List<TransactionRuleResponseDTO> result = transactionRuleService.getAllRulesForUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Lidl rule");
        assertThat(result.get(0).getPriority()).isEqualTo(100);
    }

    @Test
    public void getAllRulesForUser_returnsEmptyList_whenUserHasNoRules() {
        when(transactionRuleRepository.findByUserIdAndActiveTrueOrderByPriorityDesc(userId))
                .thenReturn(List.of());

        assertThat(transactionRuleService.getAllRulesForUser(userId)).isEmpty();
    }

    // getRuleById

    @Test
    public void getRuleById_returnsDTO_whenRuleExists() {
        when(transactionRuleRepository.findByIdAndUserId(ruleId, userId))
                .thenReturn(Optional.of(testRule));
        when(transactionRuleMapper.toResponseDTO(testRule)).thenReturn(testResponseDTO);

        TransactionRuleResponseDTO result = transactionRuleService.getRuleById(ruleId, userId);

        assertThat(result.getName()).isEqualTo("Lidl rule");
    }

    @Test
    public void getRuleById_throwsException_whenRuleNotFound() {
        when(transactionRuleRepository.findByIdAndUserId(ruleId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionRuleService.getRuleById(ruleId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rule not found");
    }

    @Test
    public void getRuleById_throwsException_whenRuleBelongsToDifferentUser() {
        UUID differentUserId = UUID.randomUUID();
        when(transactionRuleRepository.findByIdAndUserId(ruleId, differentUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionRuleService.getRuleById(ruleId, differentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rule not found");
    }

    // createRule

    @Test
    public void createRule_savesAndReturnsDTO() {
        TransactionRuleCreateDTO createDTO = new TransactionRuleCreateDTO();
        createDTO.setName("Lidl rule");
        createDTO.setMatchField(RuleMatchField.MERCHANT);
        createDTO.setMatchPattern("lidl");
        createDTO.setCategoryId(categoryId);
        createDTO.setPriority(100);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(foodCategory));
        when(transactionRuleMapper.toEntity(createDTO, foodCategory, userId)).thenReturn(testRule);
        when(transactionRuleRepository.save(testRule)).thenReturn(testRule);
        when(transactionRuleMapper.toResponseDTO(testRule)).thenReturn(testResponseDTO);

        TransactionRuleResponseDTO result = transactionRuleService.createRule(userId, createDTO);

        assertThat(result.getName()).isEqualTo("Lidl rule");
        verify(transactionRuleRepository, times(1)).save(testRule);
    }

    @Test
    public void createRule_throwsException_whenCategoryNotFound() {
        TransactionRuleCreateDTO createDTO = new TransactionRuleCreateDTO();
        createDTO.setCategoryId(categoryId);
        createDTO.setMatchField(RuleMatchField.MERCHANT);
        createDTO.setMatchPattern("lidl");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionRuleService.createRule(userId, createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");

        verify(transactionRuleRepository, never()).save(any());
    }

    // updateRule

    @Test
    public void updateRule_appliesChangesAndReturnsDTO() {
        TransactionRuleUpdateDTO updateDTO = new TransactionRuleUpdateDTO();
        updateDTO.setName("Updated Lidl rule");
        updateDTO.setPriority(200);

        TransactionRuleResponseDTO updatedResponse = new TransactionRuleResponseDTO();
        updatedResponse.setName("Updated Lidl rule");
        updatedResponse.setPriority(200);

        when(transactionRuleRepository.findByIdAndUserId(ruleId, userId))
                .thenReturn(Optional.of(testRule));
        when(transactionRuleMapper.applyUpdate(testRule, updateDTO, null)).thenReturn(testRule);
        when(transactionRuleRepository.save(testRule)).thenReturn(testRule);
        when(transactionRuleMapper.toResponseDTO(testRule)).thenReturn(updatedResponse);

        TransactionRuleResponseDTO result = transactionRuleService.updateRule(ruleId, userId, updateDTO);

        assertThat(result.getName()).isEqualTo("Updated Lidl rule");
        verify(transactionRuleMapper, times(1)).applyUpdate(testRule, updateDTO, null);
        verify(transactionRuleRepository, times(1)).save(testRule);
    }

    @Test
    public void updateRule_throwsException_whenRuleNotFound() {
        when(transactionRuleRepository.findByIdAndUserId(ruleId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionRuleService.updateRule(ruleId, userId, new TransactionRuleUpdateDTO()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rule not found");

        verify(transactionRuleRepository, never()).save(any());
    }

    // deleteRule

    @Test
    public void deleteRule_softDeletes_setsActiveFalse() {
        when(transactionRuleRepository.findByIdAndUserId(ruleId, userId))
                .thenReturn(Optional.of(testRule));

        transactionRuleService.deleteRule(ruleId, userId);

        assertThat(testRule.isActive()).isFalse();
        verify(transactionRuleRepository, times(1)).save(testRule);
        // Soft delete - must never call hard delete
        verify(transactionRuleRepository, never()).delete(any());
    }

    @Test
    public void deleteRule_throwsException_whenRuleNotFound() {
        when(transactionRuleRepository.findByIdAndUserId(ruleId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionRuleService.deleteRule(ruleId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rule not found");

        verify(transactionRuleRepository, never()).save(any());
    }
}