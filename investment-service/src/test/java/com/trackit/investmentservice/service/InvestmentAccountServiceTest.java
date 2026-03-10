package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.InvestmentAccountCreateDTO;
import com.trackit.investmentservice.dto.InvestmentAccountResponseDTO;
import com.trackit.investmentservice.dto.InvestmentAccountUpdateDTO;
import com.trackit.investmentservice.mapper.InvestmentAccountMapper;
import com.trackit.investmentservice.model.AccountType;
import com.trackit.investmentservice.model.InvestmentAccount;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class InvestmentAccountServiceTest {

    @Mock
    private InvestmentAccountRepository investmentAccountRepository;

    @Mock
    private InvestmentAccountMapper investmentAccountMapper;

    @InjectMocks
    private InvestmentAccountService investmentAccountService;

    private UUID userId;
    private UUID accountId;
    private InvestmentAccount testAccount;
    private InvestmentAccountResponseDTO testResponseDTO;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        //Build reusable test entity
        testAccount = new InvestmentAccount();
        testAccount.setUserId(userId);
        testAccount.setAccountType(AccountType.IKE);
        testAccount.setBrokerName("XTB");
        testAccount.setDisplayName("Moje IKE");
        testAccount.setCurrency("PLN");
        testAccount.setActive(true);

        //Build reusable test response DTO
        testResponseDTO = new InvestmentAccountResponseDTO();
        testResponseDTO.setId(accountId.toString());
        testResponseDTO.setAccountType("IKE");
        testResponseDTO.setBrokerName("XTB");
        testResponseDTO.setDisplayName("Moje IKE");
        testResponseDTO.setCurrency("PLN");
        testResponseDTO.setActive(true);
    }

    //getAllAcountsForUser

    @Test
    void getAllAccountsForUser_returnsListOfDTOs() {
        when(investmentAccountRepository.findByUserId(userId))
                .thenReturn(List.of(testAccount));
        when(investmentAccountMapper.toResponseDTO(testAccount))
                .thenReturn(testResponseDTO);

        List<InvestmentAccountResponseDTO> result = investmentAccountService.getAllAccountsForUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBrokerName()).isEqualTo("XTB");
        assertThat(result.get(0).getAccountType()).isEqualTo("IKE");
    }

    @Test
    void getAllAccountsForUser_returnsEmptyList_whenUserHasNoAccounts() {
        when(investmentAccountRepository.findByUserId(userId))
                .thenReturn(List.of());

        List<InvestmentAccountResponseDTO> result = investmentAccountService.getAllAccountsForUser(userId);

        assertThat(result).isEmpty();
    }

    //getAccountById

    @Test
    void getAccountById_returnsDTO_whenAccountExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(investmentAccountMapper.toResponseDTO(testAccount))
                .thenReturn(testResponseDTO);

        InvestmentAccountResponseDTO result = investmentAccountService.getAccountById(accountId, userId);

        assertThat(result.getId()).isEqualTo(accountId.toString());
        assertThat(result.getBrokerName()).isEqualTo("XTB");
    }

    @Test
    void getAccountById_throwsExveption_whenAccountNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        //Assert that exception is thrown with correct mesage
        assertThatThrownBy(() -> investmentAccountService.getAccountById(accountId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void getAccountById_throwsException_whenAccountBelongsToDifferentUser() {
        UUID differentUserId = UUID.randomUUID();

        when(investmentAccountRepository.findByIdAndUserId(accountId, differentUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentAccountService.getAccountById(accountId, differentUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    //create account

    @Test
    void createAccount_savesAndReturnsDTO() {
        InvestmentAccountCreateDTO createDTO = new InvestmentAccountCreateDTO();
        createDTO.setAccountType(AccountType.IKE);
        createDTO.setBrokerName("XTB");
        createDTO.setCurrency("PLN");

        when(investmentAccountMapper.toEntity(createDTO, userId))
                .thenReturn(testAccount);
        when(investmentAccountRepository.save(testAccount))
                .thenReturn(testAccount);
        when(investmentAccountMapper.toResponseDTO(testAccount))
                .thenReturn(testResponseDTO);

        InvestmentAccountResponseDTO result = investmentAccountService.createAccount(userId, createDTO);

        assertThat(result.getBrokerName()).isEqualTo("XTB");
        assertThat(result.getAccountType()).isEqualTo("IKE");

        //Verify save was actually called once
        verify(investmentAccountRepository, times(1)).save(testAccount);
    }

    //update account

    @Test
    void updateAccount_appliesChangesAndReturnsDTO() {
        InvestmentAccountUpdateDTO updateDTO = new InvestmentAccountUpdateDTO();
        updateDTO.setDisplayName("Updated IKE");

        InvestmentAccountResponseDTO updatedResponse = new InvestmentAccountResponseDTO();
        updatedResponse.setDisplayName("Updated IKE");

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(investmentAccountMapper.applyUpdate(testAccount, updateDTO))
                .thenReturn(testAccount);
        when(investmentAccountRepository.save(testAccount))
                .thenReturn(testAccount);
        when(investmentAccountMapper.toResponseDTO(testAccount))
                .thenReturn(updatedResponse);

        InvestmentAccountResponseDTO result = investmentAccountService.updateAccount(accountId, userId, updateDTO);

        assertThat(result.getDisplayName()).isEqualTo("Updated IKE");
        verify(investmentAccountRepository, times(1)).save(testAccount);
    }

    @Test
    void updateAccount_throwsException_whenAccountNotFound() {
        InvestmentAccountUpdateDTO updateDTO = new InvestmentAccountUpdateDTO();
        updateDTO.setDisplayName("Updated IKE");

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentAccountService.updateAccount(accountId, userId, updateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    //deactivate account

    @Test
    void deactivateAccount_setsActiveFalse() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));

        investmentAccountService.deactiveAccount(accountId, userId);

        assertThat(testAccount.isActive()).isFalse();

        verify(investmentAccountRepository, times(1)).save(testAccount);
    }

    @Test
    void deactiveAccount_throwsException_whenAccountNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentAccountService.deactiveAccount(accountId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }
}
