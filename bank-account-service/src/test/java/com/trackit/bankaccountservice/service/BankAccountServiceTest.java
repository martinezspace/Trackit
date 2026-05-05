package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.BankAccountCreateDTO;
import com.trackit.bankaccountservice.dto.BankAccountResponseDTO;
import com.trackit.bankaccountservice.dto.BankAccountUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.BankAccountMapper;
import com.trackit.bankaccountservice.model.BankAccount;
import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.repository.BankAccountRepository;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private BankConnectionRepository bankConnectionRepository;

    @Mock
    private BankAccountMapper bankAccountMapper;

    @InjectMocks
    private BankAccountService bankAccountService;

    private UUID userId;
    private UUID accountId;
    private UUID connectionId;
    private BankAccount testAccount;
    private BankConnection testConnection;
    private BankAccountResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        connectionId = UUID.randomUUID();

        testConnection = new BankConnection();
        testConnection.setUserId(userId);
        testConnection.setInstitutionId("MONZO_GB");
        testConnection.setInstitutionName("Monzo");

        // Build reusable test entity
        testAccount = new BankAccount();
        testAccount.setConnection(testConnection);
        testAccount.setUserId(userId);
        testAccount.setExternalAccountId("ext-001");
        testAccount.setName("Current Account");
        testAccount.setCurrency("PLN");
        testAccount.setActive(true);

        // Build reusable test response DTO
        testResponseDTO = new BankAccountResponseDTO();
        testResponseDTO.setId(accountId.toString());
        testResponseDTO.setName("Current Account");
        testResponseDTO.setCurrency("PLN");
        testResponseDTO.setActive(true);
    }

    // getAllAccountsForUser

    @Test
    public void getAllAccountsForUser_returnsListOfDTOs() {
        when(bankAccountRepository.findByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(testAccount));
        when(bankAccountMapper.toResponseDTO(testAccount))
                .thenReturn(testResponseDTO);

        List<BankAccountResponseDTO> result = bankAccountService.getAllAccountsForUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Current Account");
    }

    @Test
    public void getAllAccountsForUser_returnsEmptyList_whenUserHasNoAccounts() {
        when(bankAccountRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        List<BankAccountResponseDTO> result = bankAccountService.getAllAccountsForUser(userId);

        assertThat(result).isEmpty();
    }

    // getAccountById

    @Test
    public void getAccountById_returnsDTO_whenAccountExists() {
        when(bankAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(bankAccountMapper.toResponseDTO(testAccount)).thenReturn(testResponseDTO);

        BankAccountResponseDTO result = bankAccountService.getAccountById(accountId, userId);

        assertThat(result.getName()).isEqualTo("Current Account");
    }

    @Test
    public void getAccountById_throwsException_whenAccountNotFound() {
        when(bankAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankAccountService.getAccountById(accountId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    public void getAccountById_throwsException_whenAccountBelongsToDifferentUser() {
        UUID differentUserId = UUID.randomUUID();
        when(bankAccountRepository.findByIdAndUserId(accountId, differentUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankAccountService.getAccountById(accountId, differentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    // createAccount

    @Test
    public void createAccount_savesAndReturnsDTO_whenConnectionBelongsToUser() {
        BankAccountCreateDTO createDTO = new BankAccountCreateDTO();
        createDTO.setConnectionId(connectionId);
        createDTO.setExternalAccountId("ext-001");
        createDTO.setName("Current Account");

        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.of(testConnection));
        when(bankAccountRepository.findByConnectionIdAndExternalAccountId(any(), eq("ext-001")))
                .thenReturn(Optional.empty());
        when(bankAccountMapper.toEntity(createDTO, testConnection, userId)).thenReturn(testAccount);
        when(bankAccountRepository.save(testAccount)).thenReturn(testAccount);
        when(bankAccountMapper.toResponseDTO(testAccount)).thenReturn(testResponseDTO);

        BankAccountResponseDTO result = bankAccountService.createAccount(userId, createDTO);

        assertThat(result.getName()).isEqualTo("Current Account");
        verify(bankAccountRepository, times(1)).save(testAccount);
    }

    @Test
    public void createAccount_throwsException_whenConnectionDoesNotBelongToUser() {
        BankAccountCreateDTO createDTO = new BankAccountCreateDTO();
        createDTO.setConnectionId(connectionId);

        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankAccountService.createAccount(userId, createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Connection not found");

        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    public void createAccount_returnsExistingAccount_whenExternalIdAlreadyImported() {
        BankAccountCreateDTO createDTO = new BankAccountCreateDTO();
        createDTO.setConnectionId(connectionId);
        createDTO.setExternalAccountId("ext-001");

        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.of(testConnection));
        when(bankAccountRepository.findByConnectionIdAndExternalAccountId(any(), eq("ext-001")))
                .thenReturn(Optional.of(testAccount));
        when(bankAccountMapper.toResponseDTO(testAccount)).thenReturn(testResponseDTO);

        BankAccountResponseDTO result = bankAccountService.createAccount(userId, createDTO);

        assertThat(result).isEqualTo(testResponseDTO);
        // Should not save a duplicate
        verify(bankAccountRepository, never()).save(any());
    }

    // updateAccount

    @Test
    public void updateAccount_appliesChangesAndReturnsDTO() {
        BankAccountUpdateDTO updateDTO = new BankAccountUpdateDTO();
        updateDTO.setDisplayName("My Monzo Account");

        BankAccountResponseDTO updatedResponse = new BankAccountResponseDTO();
        updatedResponse.setDisplayName("My Monzo Account");

        when(bankAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(bankAccountMapper.applyUpdate(testAccount, updateDTO)).thenReturn(testAccount);
        when(bankAccountRepository.save(testAccount)).thenReturn(testAccount);
        when(bankAccountMapper.toResponseDTO(testAccount)).thenReturn(updatedResponse);

        BankAccountResponseDTO result = bankAccountService.updateAccount(accountId, userId, updateDTO);

        assertThat(result.getDisplayName()).isEqualTo("My Monzo Account");
        verify(bankAccountMapper, times(1)).applyUpdate(testAccount, updateDTO);
        verify(bankAccountRepository, times(1)).save(testAccount);
    }

    @Test
    public void updateAccount_throwsException_whenAccountNotFound() {
        when(bankAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankAccountService.updateAccount(accountId, userId, new BankAccountUpdateDTO()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");

        verify(bankAccountRepository, never()).save(any());
    }

    // updateBalance

    @Test
    public void updateBalance_updatesBalanceFieldsAndSaves() {
        when(bankAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(testAccount)).thenReturn(testAccount);
        when(bankAccountMapper.toResponseDTO(testAccount)).thenReturn(testResponseDTO);

        bankAccountService.updateBalance(accountId, userId, new BigDecimal("1500.00"), new BigDecimal("1200.00"));

        assertThat(testAccount.getCurrentBalance()).isEqualByComparingTo("1500.00");
        assertThat(testAccount.getAvailableBalance()).isEqualByComparingTo("1200.00");
        assertThat(testAccount.getBalanceUpdatedAt()).isNotNull();
        verify(bankAccountRepository, times(1)).save(testAccount);
    }

    @Test
    public void updateBalance_throwsException_whenAccountNotFound() {
        when(bankAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankAccountService.updateBalance(accountId, userId, BigDecimal.TEN, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    // deactivateAccount

    @Test
    public void deactivateAccount_setsActiveFalse() {
        when(bankAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));

        bankAccountService.deactivateAccount(accountId, userId);

        assertThat(testAccount.isActive()).isFalse();
        verify(bankAccountRepository, times(1)).save(testAccount);
    }

    @Test
    public void deactivateAccount_throwsException_whenAccountNotFound() {
        when(bankAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankAccountService.deactivateAccount(accountId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");

        verify(bankAccountRepository, never()).save(any());
    }
}