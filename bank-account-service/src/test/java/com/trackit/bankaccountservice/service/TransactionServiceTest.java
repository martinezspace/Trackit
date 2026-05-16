package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.TransactionCreateDTO;
import com.trackit.bankaccountservice.dto.TransactionResponseDTO;
import com.trackit.bankaccountservice.dto.TransactionUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.TransactionMapper;
import com.trackit.bankaccountservice.model.*;
import com.trackit.bankaccountservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private BankAccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TransactionRuleRepository ruleRepository;
    @Mock private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private UUID userId;
    private UUID accountId;
    private UUID transactionId;
    private BankAccount testAccount;
    private Transaction testTransaction;
    private TransactionResponseDTO testResponseDTO;
    private Category foodCategory;
    private TransactionRule lidlRule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        // Build reusable test account
        testAccount = new BankAccount();
        testAccount.setUserId(userId);
        testAccount.setExternalAccountId("ext-001");
        testAccount.setCurrency("PLN");
        testAccount.setActive(true);

        // Build reusable test transaction
        testTransaction = new Transaction();
        testTransaction.setAccount(testAccount);
        testTransaction.setUserId(userId);
        testTransaction.setAmount(new BigDecimal("99.99"));
        testTransaction.setDirection(TransactionDirection.OUTBOUND);
        testTransaction.setCurrency("PLN");
        testTransaction.setBookingDate(LocalDate.now());
        testTransaction.setCategorizationStatus(CategorizationStatus.UNCATEGORIZED);
        testTransaction.setManual(false);

        // Build reusable test response DTO
        testResponseDTO = new TransactionResponseDTO();
        testResponseDTO.setId(transactionId.toString());
        testResponseDTO.setAmount("99.99");
        testResponseDTO.setDirection("OUTBOUND");

        // Build reusable food category
        foodCategory = new Category();
        foodCategory.setName("Food & Dining");
        foodCategory.setType(CategoryType.DEBIT);

        // Build reusable Lidl merchant rule
        lidlRule = new TransactionRule();
        lidlRule.setUserId(userId);
        lidlRule.setMatchField(RuleMatchField.MERCHANT);
        lidlRule.setMatchPattern("lidl");
        lidlRule.setCategory(foodCategory);
        lidlRule.setPriority(100);
        lidlRule.setActive(true);
    }

    // getTransactionById

    @Test
    public void getTransactionById_returnsDTO_whenTransactionExists() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(testTransaction));
        when(transactionMapper.toResponseDTO(testTransaction)).thenReturn(testResponseDTO);

        TransactionResponseDTO result = transactionService.getTransactionById(transactionId, userId);

        assertThat(result.getAmount()).isEqualTo("99.99");
        assertThat(result.getDirection()).isEqualTo("OUTBOUND");
    }

    @Test
    public void getTransactionById_throwsException_whenTransactionNotFound() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(transactionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    public void getTransactionById_throwsException_whenTransactionBelongsToDifferentUser() {
        UUID differentUserId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(transactionId, differentUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(transactionId, differentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    // createTransaction

    @Test
    public void createTransaction_throwsException_whenAccountDoesNotBelongToUser() {
        TransactionCreateDTO createDTO = buildCreateDTO(accountId, null);
        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(userId, createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void createTransaction_returnsDuplicate_whenExternalIdAlreadyExists() {
        TransactionCreateDTO createDTO = buildCreateDTO(accountId, "ext-tx-001");

        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountIdAndExternalId(any(), eq("ext-tx-001")))
                .thenReturn(Optional.of(testTransaction));
        when(transactionMapper.toResponseDTO(testTransaction)).thenReturn(testResponseDTO);

        TransactionResponseDTO result = transactionService.createTransaction(userId, createDTO);

        assertThat(result).isEqualTo(testResponseDTO);
        // Should not save a duplicate
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void createTransaction_autoCategorizesViaMerchantRule_whenPatternMatches() {
        TransactionCreateDTO createDTO = buildCreateDTO(accountId, null);
        createDTO.setMerchantName("Lidl Polska sp. z o.o.");
        testTransaction.setMerchantName("Lidl Polska sp. z o.o.");

        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(ruleRepository.findByUserIdAndActiveTrueOrderByPriorityDesc(userId)).thenReturn(List.of(lidlRule));
        when(transactionMapper.toEntity(createDTO, testAccount, userId)).thenReturn(testTransaction);
        when(transactionRepository.save(any())).thenReturn(testTransaction);
        when(transactionMapper.toResponseDTO(testTransaction)).thenReturn(testResponseDTO);

        transactionService.createTransaction(userId, createDTO);

        assertThat(testTransaction.getCategory()).isEqualTo(foodCategory);
        assertThat(testTransaction.getCategorizationStatus()).isEqualTo(CategorizationStatus.AUTO);
    }

    @Test
    public void createTransaction_remainsUncategorized_whenNoRulesMatch() {
        TransactionCreateDTO createDTO = buildCreateDTO(accountId, null);
        createDTO.setMerchantName("Unknown Shop XYZ");

        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(ruleRepository.findByUserIdAndActiveTrueOrderByPriorityDesc(userId)).thenReturn(List.of());
        when(transactionMapper.toEntity(createDTO, testAccount, userId)).thenReturn(testTransaction);
        when(transactionRepository.save(any())).thenReturn(testTransaction);
        when(transactionMapper.toResponseDTO(testTransaction)).thenReturn(testResponseDTO);

        transactionService.createTransaction(userId, createDTO);

        assertThat(testTransaction.getCategory()).isNull();
        assertThat(testTransaction.getCategorizationStatus()).isEqualTo(CategorizationStatus.UNCATEGORIZED);
    }

    @Test
    public void createTransaction_setsManualCategory_whenExplicitCategoryIdProvided() {
        UUID categoryId = UUID.randomUUID();
        TransactionCreateDTO createDTO = buildCreateDTO(accountId, null);
        createDTO.setCategoryId(categoryId);

        when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccountIdAndExternalId(any(), any())).thenReturn(Optional.empty());
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(foodCategory));
        when(transactionMapper.toEntity(createDTO, testAccount, userId)).thenReturn(testTransaction);
        when(transactionRepository.save(any())).thenReturn(testTransaction);
        when(transactionMapper.toResponseDTO(testTransaction)).thenReturn(testResponseDTO);

        transactionService.createTransaction(userId, createDTO);

        assertThat(testTransaction.getCategory()).isEqualTo(foodCategory);
        assertThat(testTransaction.getCategorizationStatus()).isEqualTo(CategorizationStatus.MANUAL);
    }

    // updateTransaction

    @Test
    public void updateTransaction_appliesChangesAndReturnsDTO() {
        UUID categoryId = UUID.randomUUID();
        TransactionUpdateDTO updateDTO = new TransactionUpdateDTO();
        updateDTO.setCategoryId(categoryId);
        updateDTO.setNotes("Grocery run");

        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(testTransaction));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(foodCategory));
        when(transactionMapper.applyUpdate(testTransaction, updateDTO, foodCategory))
                .thenReturn(testTransaction);
        when(transactionRepository.save(testTransaction)).thenReturn(testTransaction);
        when(transactionMapper.toResponseDTO(testTransaction)).thenReturn(testResponseDTO);

        transactionService.updateTransaction(transactionId, userId, updateDTO);

        verify(transactionMapper, times(1)).applyUpdate(testTransaction, updateDTO, foodCategory);
        verify(transactionRepository, times(1)).save(testTransaction);
    }

    @Test
    public void updateTransaction_throwsException_whenTransactionNotFound() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, userId, new TransactionUpdateDTO()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void updateTransaction_throwsException_whenCategoryNotFound() {
        UUID categoryId = UUID.randomUUID();
        TransactionUpdateDTO updateDTO = new TransactionUpdateDTO();
        updateDTO.setCategoryId(categoryId);

        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(testTransaction));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.updateTransaction(transactionId, userId, updateDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    // deleteTransaction

    @Test
    public void deleteTransaction_deletesWhenManual() {
        testTransaction.setManual(true);
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(testTransaction));

        transactionService.deleteTransaction(transactionId, userId);

        verify(transactionRepository, times(1)).delete(testTransaction);
    }

    @Test
    public void deleteTransaction_throwsException_whenTransactionIsNotManual() {
        testTransaction.setManual(false);
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.of(testTransaction));

        assertThatThrownBy(() -> transactionService.deleteTransaction(transactionId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manual");

        verify(transactionRepository, never()).delete(any());
    }

    @Test
    public void deleteTransaction_throwsException_whenTransactionNotFound() {
        when(transactionRepository.findByIdAndUserId(transactionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(transactionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    // reapplyRules

    @Test
    public void reapplyRules_returnsZero_whenUserHasNoActiveRules() {
        when(ruleRepository.findByUserIdAndActiveTrueOrderByPriorityDesc(userId)).thenReturn(List.of());

        int count = transactionService.reapplyRules(userId);

        assertThat(count).isEqualTo(0);
        verify(transactionRepository, never()).save(any());
    }

    private TransactionCreateDTO buildCreateDTO(UUID accountId, String externalId) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setAccountId(accountId);
        dto.setExternalId(externalId);
        dto.setAmount(new BigDecimal("99.99"));
        dto.setDirection(TransactionDirection.OUTBOUND);
        dto.setCurrency("PLN");
        dto.setBookingDate(LocalDate.now());
        return dto;
    }
}