package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.HoldingResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.HoldingMapper;
import com.trackit.investmentservice.model.*;
import com.trackit.investmentservice.repository.HoldingRepository;
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

class HoldingServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private InvestmentAccountRepository investmentAccountRepository;

    @Mock
    private InvestmentTransactionRepository transactionRepository;

    @Mock
    private HoldingMapper holdingMapper;

    @InjectMocks
    private HoldingService holdingService;

    private UUID userId;
    private UUID accountId;
    private UUID instrumentId;
    private InvestmentAccount testAccount;
    private Instrument testInstrument;
    private Holding testHolding;
    private HoldingResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        instrumentId = UUID.randomUUID();

        testAccount = new InvestmentAccount();
        testAccount.setUserId(userId);
        testAccount.setBrokerName("Trading212");
        testAccount.setAccountType(AccountType.BROKERAGE);
        testAccount.setCurrency("PLN");

        testInstrument = new Instrument();
        testInstrument.setIsin("IE00B5BMR087");
        testInstrument.setTicker("CSPX");
        testInstrument.setName("iShares Core S&P 500");
        testInstrument.setInstrumentType(InstrumentType.ETF);
        testInstrument.setCurrency("USD");

        testHolding = new Holding();
        testHolding.setAccount(testAccount);
        testHolding.setInstrument(testInstrument);
        testHolding.setQuantity(new BigDecimal("1.150000"));
        testHolding.setAvgPurchasePrice(new BigDecimal("516.7500"));
        testHolding.setTotalInvested(new BigDecimal("2399.99"));
        testHolding.setCurrency("PLN");

        testResponseDTO = new HoldingResponseDTO();
        testResponseDTO.setAccountId(accountId.toString());
        testResponseDTO.setInstrumentTicker("CSPX");
        testResponseDTO.setQuantity("1.150000");
        testResponseDTO.setTotalInvested("2399.99");
    }

    // getAllHoldingsForAccount
    @Test
    void getAllHoldingsForAccount_returnsList_whenAccountExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(holdingRepository.findByAccount_Id(accountId))
                .thenReturn(List.of(testHolding));
        when(holdingMapper.toResponseDTO(testHolding))
                .thenReturn(testResponseDTO);

        List<HoldingResponseDTO> result = holdingService.getAllHoldingsForAccount(accountId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstrumentTicker()).isEqualTo("CSPX");
    }

    @Test
    void getAllHoldingsForAccount_returnsEmptyList_whenNoHoldings() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(holdingRepository.findByAccount_Id(accountId))
                .thenReturn(List.of());

        List<HoldingResponseDTO> result = holdingService.getAllHoldingsForAccount(accountId, userId);

        assertThat(result).isEmpty();
    }

    @Test
    void getAllHoldingsForAccount_throwsException_whenAccountNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> holdingService.getAllHoldingsForAccount(accountId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account Not Found");
    }

    // getHoldingByInstrument
    @Test
    void getHoldingByInstrument_returnsDTO_whenExists() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(holdingRepository.findByAccount_IdAndInstrument_Id(accountId, instrumentId))
                .thenReturn(Optional.of(testHolding));
        when(holdingMapper.toResponseDTO(testHolding))
                .thenReturn(testResponseDTO);

        HoldingResponseDTO result = holdingService.getHoldingByInstrument(accountId, instrumentId, userId);

        assertThat(result.getInstrumentTicker()).isEqualTo("CSPX");
        assertThat(result.getQuantity()).isEqualTo("1.150000");
    }

    @Test
    void getHoldingByInstrument_throwsException_whenNotFound() {
        when(investmentAccountRepository.findByIdAndUserId(accountId, userId))
                .thenReturn(Optional.of(testAccount));
        when(holdingRepository.findByAccount_IdAndInstrument_Id(accountId, instrumentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> holdingService.getHoldingByInstrument(accountId, instrumentId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Holding Not Found For Instrument");
    }

    // recalculateHoldings — BUY only
    @Test
    void recalculateHoldings_createsBuyHolding_whenNoExistingHolding() {
        InvestmentTransaction buyTx = buildTransaction(TransactionType.BUY,
                new BigDecimal("2.000000"), new BigDecimal("500.0000"), new BigDecimal("1000.00"));

        when(investmentAccountRepository.findById(accountId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccount_IdAndCancelledFalse(accountId))
                .thenReturn(List.of(buyTx));
        when(holdingRepository.findByAccount_IdAndInstrument_Id(any(), any()))
                .thenReturn(Optional.empty());
        when(holdingRepository.findByAccount_Id(accountId))
                .thenReturn(List.of());

        holdingService.recalculateHoldings(accountId);

        verify(holdingRepository, times(1)).save(argThat(h ->
                h.getQuantity().compareTo(new BigDecimal("2.000000")) == 0 &&
                        h.getTotalInvested().compareTo(new BigDecimal("1000.00")) == 0
        ));
    }

    // recalculateHoldings — BUY + SELL
    @Test
    void recalculateHoldings_updatesQuantityAndInvested_afterBuyAndSell() {
        InvestmentTransaction buyTx = buildTransaction(TransactionType.BUY,
                new BigDecimal("10.000000"), new BigDecimal("100.0000"), new BigDecimal("1000.00"));
        InvestmentTransaction sellTx = buildTransaction(TransactionType.SELL,
                new BigDecimal("4.000000"), new BigDecimal("120.0000"), new BigDecimal("480.00"));

        when(investmentAccountRepository.findById(accountId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccount_IdAndCancelledFalse(accountId))
                .thenReturn(List.of(buyTx, sellTx));
        when(holdingRepository.findByAccount_IdAndInstrument_Id(any(), any()))
                .thenReturn(Optional.empty());
        when(holdingRepository.findByAccount_Id(accountId))
                .thenReturn(List.of());

        holdingService.recalculateHoldings(accountId);

        verify(holdingRepository, times(1)).save(argThat(h ->
                // quantity = 10 - 4 = 6
                h.getQuantity().compareTo(new BigDecimal("6.000000")) == 0 &&
                        // avgPurchasePrice stays at 100 PLN
                        h.getAvgPurchasePrice().compareTo(new BigDecimal("100.0000")) == 0
        ));
    }

    // recalculateHoldings — zero out when all transactions cancelled
    @Test
    void recalculateHoldings_zeroesOutHolding_whenNoActiveTransactions() {
        // No active transactions
        when(investmentAccountRepository.findById(accountId))
                .thenReturn(Optional.of(testAccount));
        when(transactionRepository.findByAccount_IdAndCancelledFalse(accountId))
                .thenReturn(List.of());

        // But holding still exists from previous import
        when(holdingRepository.findByAccount_Id(accountId))
                .thenReturn(List.of(testHolding));

        holdingService.recalculateHoldings(accountId);

        // Holding should be zeroed out
        assertThat(testHolding.getQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(testHolding.getTotalInvested()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(holdingRepository, times(1)).save(testHolding);
    }

    @Test
    void recalculateHoldings_throwsException_whenAccountNotFound() {
        when(investmentAccountRepository.findById(accountId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> holdingService.recalculateHoldings(accountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account Not Found");
    }

    // Private helper
    private InvestmentTransaction buildTransaction(
            TransactionType type, BigDecimal quantity, BigDecimal price, BigDecimal amount) {
        InvestmentTransaction tx = new InvestmentTransaction();
        tx.setAccount(testAccount);
        tx.setInstrument(testInstrument);
        tx.setTransactionType(type);
        tx.setQuantity(quantity);
        tx.setPrice(price);
        tx.setAmount(amount);
        tx.setCurrency("PLN");
        tx.setTransactionDate(LocalDate.of(2024, 1, 30));
        tx.setCancelled(false);
        return tx;
    }
}