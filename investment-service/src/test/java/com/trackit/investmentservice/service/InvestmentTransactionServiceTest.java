package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.InvestmentTransactionResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.InvestmentTransactionMapper;
import com.trackit.investmentservice.model.*;
import com.trackit.investmentservice.repository.InvestmentAccountRepository;
import com.trackit.investmentservice.repository.InvestmentTransactionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvestmentTransactionServiceTest {

    @Mock
    private InvestmentTransactionRepository transactionRepository;

    @Mock
    private InvestmentAccountRepository investmentAccountRepository;

    @Mock
    private InvestmentTransactionMapper transactionMapper;

    @InjectMocks
    private InvestmentTransactionService transactionService;

    private UUID userId;
    private UUID accountId;
    private UUID transactionId;
    private UUID instrumentId;
    private InvestmentAccount testAccount;
    private InvestmentTransaction testTransaction;
    private InvestmentTransactionResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
        instrumentId = UUID.randomUUID();

        testAccount = new InvestmentAccount();
        testAccount.setUserId(userId);
        testAccount.setBrokerName("Trading212");
        testAccount.setAccountType(AccountType.BROKERAGE);
        testAccount.setCurrency("PLN");

        Instrument testInstrument = new Instrument();
        testInstrument.setIsin("IE00B5BMR087");
        testInstrument.setTicker("CSPX");
        testInstrument.setName("iShares Core S&P 500");
        testInstrument.setInstrumentType(InstrumentType.ETF);
        testInstrument.setCurrency("USD");

        testTransaction = new InvestmentTransaction();
        testTransaction.setAccount(testAccount);
        testTransaction.setInstrument(testInstrument);
        testTransaction.setExternalId("EOF9554472121");
        testTransaction.setTransactionType(TransactionType.BUY);
        testTransaction.setQuantity(new BigDecimal("1.150000"));
        testTransaction.setPrice(new BigDecimal("516.7500"));
        testTransaction.setAmount(new BigDecimal("2399.99"));
        testTransaction.setCurrency("PLN");
        testTransaction.setTransactionDate(LocalDate.of(2024, 1, 30));
        testTransaction.setCancelled(false);

        testResponseDTO = new InvestmentTransactionResponseDTO();
        testResponseDTO.setId(transactionId.toString());
        testResponseDTO.setAccountId(accountId.toString());
        testResponseDTO.setTransactionType("BUY");
        testResponseDTO.setQuantity("1.150000");
        testResponseDTO.setPrice("516.7500");
        testResponseDTO.setAmount("2399.99");
        testResponseDTO.setCurrency("PLN");
        testResponseDTO.setCancelled(false);
    }

    // getAllTransactionsForAccount
    @Test
    void getAllTransactionsForAccount_returnsList_whenAccountExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccount_IdAndCancelledFalse(accountId))
                .thenReturn(List.of(testTransaction));
        when(transactionMapper.toResponseDTO(testTransaction))
                .thenReturn(testResponseDTO);

        List<InvestmentTransactionResponseDTO> result =
                transactionService.getAllTransactionsForAccount(accountId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionType()).isEqualTo("BUY");
        assertThat(result.get(0).isCancelled()).isFalse();
    }

    @Test
    void getAllTransactionsForAccount_throwsException_whenAccountNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getAllTransactionsForAccount(accountId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account Not Found");
    }

    // getTransactionsByInstrument
    @Test
    void getTransactionsByInstrument_returnsList_whenExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccount_IdAndInstrument_IdAndCancelledFalse(accountId, instrumentId))
                .thenReturn(List.of(testTransaction));
        when(transactionMapper.toResponseDTO(testTransaction))
                .thenReturn(testResponseDTO);

        List<InvestmentTransactionResponseDTO> result =
                transactionService.getTransactionsByInstrument(accountId, instrumentId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionType()).isEqualTo("BUY");
    }

    @Test
    void getTransactionsByInstrument_returnsEmpty_whenNoneExist() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccount_IdAndInstrument_IdAndCancelledFalse(accountId, instrumentId))
                .thenReturn(List.of());

        List<InvestmentTransactionResponseDTO> result =
                transactionService.getTransactionsByInstrument(accountId, instrumentId, userId);

        assertThat(result).isEmpty();
    }

    // getTransactionById
    @Test
    void getTransactionById_returnsDTO_whenExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByIdAndAccount_Id(transactionId, accountId))
                .thenReturn(Optional.of(testTransaction));
        when(transactionMapper.toResponseDTO(testTransaction))
                .thenReturn(testResponseDTO);

        InvestmentTransactionResponseDTO result =
                transactionService.getTransactionById(transactionId, accountId, userId);

        assertThat(result.getTransactionType()).isEqualTo("BUY");
        assertThat(result.getAmount()).isEqualTo("2399.99");
    }

    @Test
    void getTransactionById_throwsException_whenNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByIdAndAccount_Id(transactionId, accountId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(transactionId, accountId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction Not Found");
    }

    // cancelTransaction
    @Test
    void cancelTransaction_setsCancelledTrue() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByIdAndAccount_Id(transactionId, accountId))
                .thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(testTransaction))
                .thenReturn(testTransaction);

        InvestmentTransactionResponseDTO cancelledResponse = new InvestmentTransactionResponseDTO();
        cancelledResponse.setCancelled(true);
        when(transactionMapper.toResponseDTO(testTransaction))
                .thenReturn(cancelledResponse);

        InvestmentTransactionResponseDTO result =
                transactionService.cancelTransaction(transactionId, accountId, userId);

        assertThat(testTransaction.isCancelled()).isTrue();
        assertThat(result.isCancelled()).isTrue();
        verify(transactionRepository, times(1)).save(testTransaction);
    }

    @Test
    void cancelTransaction_throwsException_whenAlreadyCancelled() {
        testTransaction.setCancelled(true);

        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByIdAndAccount_Id(transactionId, accountId))
                .thenReturn(Optional.of(testTransaction));

        assertThatThrownBy(() -> transactionService.cancelTransaction(transactionId, accountId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transaction is already cancelled");

        verify(transactionRepository, never()).save(any());
    }

    // cancelAllTransactionsForBatch
    @Test
    void cancelAllTransactionsForBatch_cancelsAllTransactions() {
        UUID batchId = UUID.randomUUID();
        InvestmentTransaction tx1 = new InvestmentTransaction();
        InvestmentTransaction tx2 = new InvestmentTransaction();
        tx1.setCancelled(false);
        tx2.setCancelled(false);

        when(transactionRepository.findByBatch_Id(batchId))
                .thenReturn(List.of(tx1, tx2));

        transactionService.cancelAllTransactionsForBatch(batchId);

        assertThat(tx1.isCancelled()).isTrue();
        assertThat(tx2.isCancelled()).isTrue();
        verify(transactionRepository, times(1)).saveAll(List.of(tx1, tx2));
    }

    @Test
    void cancelAllTransactionsForBatch_doesNothing_whenNoBatchTransactions() {
        UUID batchId = UUID.randomUUID();

        when(transactionRepository.findByBatch_Id(batchId))
                .thenReturn(List.of());

        transactionService.cancelAllTransactionsForBatch(batchId);

        verify(transactionRepository, never()).saveAll(any());
    }
}