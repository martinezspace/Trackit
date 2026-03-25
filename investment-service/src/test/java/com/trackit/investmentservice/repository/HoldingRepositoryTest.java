package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class HoldingRepositoryTest {

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private InvestmentAccountRepository investmentAccountRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    private InvestmentAccount account;
    private InvestmentAccount otherAccount;
    private Instrument cspx;
    private Instrument sxrv;
    private Holding cspxHolding;

    @BeforeEach
    void setUp() {
        account = new InvestmentAccount();
        account.setUserId(UUID.randomUUID());
        account.setAccountType(AccountType.BROKERAGE);
        account.setBrokerName("Trading212");
        account.setCurrency("PLN");
        account.setActive(true);
        investmentAccountRepository.save(account);

        otherAccount = new InvestmentAccount();
        otherAccount.setUserId(UUID.randomUUID());
        otherAccount.setAccountType(AccountType.BROKERAGE);
        otherAccount.setBrokerName("XTB");
        otherAccount.setCurrency("PLN");
        otherAccount.setActive(true);
        investmentAccountRepository.save(otherAccount);

        cspx = new Instrument();
        cspx.setIsin("IE00B5BMR087");
        cspx.setTicker("CSPX");
        cspx.setName("iShares Core S&P 500");
        cspx.setInstrumentType(InstrumentType.ETF);
        cspx.setCurrency("USD");
        instrumentRepository.save(cspx);

        sxrv = new Instrument();
        sxrv.setIsin("IE00B53SZB19");
        sxrv.setTicker("SXRV");
        sxrv.setName("iShares NASDAQ 100");
        sxrv.setInstrumentType(InstrumentType.ETF);
        sxrv.setCurrency("USD");
        instrumentRepository.save(sxrv);

        // CSPX holding for account
        cspxHolding = new Holding();
        cspxHolding.setAccount(account);
        cspxHolding.setInstrument(cspx);
        cspxHolding.setQuantity(new BigDecimal("1.150000"));
        cspxHolding.setAvgPurchasePrice(new BigDecimal("516.7500"));
        cspxHolding.setTotalInvested(new BigDecimal("2399.99"));
        cspxHolding.setCurrency("PLN");
        holdingRepository.save(cspxHolding);

        // SXRV holding for account
        Holding sxrvHolding = new Holding();
        sxrvHolding.setAccount(account);
        sxrvHolding.setInstrument(sxrv);
        sxrvHolding.setQuantity(new BigDecimal("0.148348"));
        sxrvHolding.setAvgPurchasePrice(new BigDecimal("924.9000"));
        sxrvHolding.setTotalInvested(new BigDecimal("600.00"));
        sxrvHolding.setCurrency("PLN");
        holdingRepository.save(sxrvHolding);

        // CSPX holding for otherAccount — should not appear in account results
        Holding otherHolding = new Holding();
        otherHolding.setAccount(otherAccount);
        otherHolding.setInstrument(cspx);
        otherHolding.setQuantity(new BigDecimal("5.000000"));
        otherHolding.setAvgPurchasePrice(new BigDecimal("500.0000"));
        otherHolding.setTotalInvested(new BigDecimal("2500.00"));
        otherHolding.setCurrency("PLN");
        holdingRepository.save(otherHolding);
    }

    // findByAccount_Id
    @Test
    void findByAccount_Id_returnsOnlyAccountHoldings() {
        List<Holding> result = holdingRepository.findByAccount_Id(account.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(h -> h.getAccount().getId().equals(account.getId()));
    }

    @Test
    void findByAccount_Id_returnsEmpty_whenNoHoldings() {
        InvestmentAccount emptyAccount = new InvestmentAccount();
        emptyAccount.setUserId(UUID.randomUUID());
        emptyAccount.setAccountType(AccountType.IKE);
        emptyAccount.setBrokerName("mBank");
        emptyAccount.setCurrency("PLN");
        emptyAccount.setActive(true);
        investmentAccountRepository.save(emptyAccount);

        List<Holding> result = holdingRepository.findByAccount_Id(emptyAccount.getId());

        assertThat(result).isEmpty();
    }

    // findByAccount_IdAndInstrument_Id
    @Test
    void findByAccount_IdAndInstrument_Id_returnsHolding_whenExists() {
        Optional<Holding> result = holdingRepository
                .findByAccount_IdAndInstrument_Id(account.getId(), cspx.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getInstrument().getTicker()).isEqualTo("CSPX");
        assertThat(result.get().getQuantity()).isEqualByComparingTo(new BigDecimal("1.150000"));
    }

    @Test
    void findByAccount_IdAndInstrument_Id_returnsEmpty_whenWrongAccount() {
        Optional<Holding> result = holdingRepository
                .findByAccount_IdAndInstrument_Id(otherAccount.getId(), sxrv.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByAccount_IdAndInstrument_Id_returnsEmpty_whenInstrumentNotHeld() {
        Instrument notHeld = new Instrument();
        notHeld.setIsin("PL0009999944");
        notHeld.setTicker("CDR");
        notHeld.setName("CD Projekt SA");
        notHeld.setInstrumentType(InstrumentType.STOCK);
        notHeld.setCurrency("PLN");
        instrumentRepository.save(notHeld);

        Optional<Holding> result = holdingRepository
                .findByAccount_IdAndInstrument_Id(account.getId(), notHeld.getId());

        assertThat(result).isEmpty();
    }

    // findByInstrument_Id
    @Test
    void findByInstrument_Id_returnsAllAccountsHoldingInstrument() {
        // Both account and otherAccount hold CSPX
        List<Holding> result = holdingRepository.findByInstrument_Id(cspx.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(h -> h.getInstrument().getId().equals(cspx.getId()));
    }

    @Test
    void findByInstrument_Id_returnsEmpty_whenNobodyHoldsInstrument() {
        Instrument notHeld = new Instrument();
        notHeld.setIsin("PL0009999944");
        notHeld.setTicker("CDR");
        notHeld.setName("CD Projekt SA");
        notHeld.setInstrumentType(InstrumentType.STOCK);
        notHeld.setCurrency("PLN");
        instrumentRepository.save(notHeld);

        List<Holding> result = holdingRepository.findByInstrument_Id(notHeld.getId());

        assertThat(result).isEmpty();
    }
}