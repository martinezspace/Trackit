package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.Instrument;
import com.trackit.investmentservice.model.InstrumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class InstrumentRepositoryTest {

    @Autowired
    private InstrumentRepository instrumentRepository;

    private Instrument stock;
    private Instrument etf;
    private Instrument fund;

    @BeforeEach
    void setUp() {
        stock = new Instrument();
        stock.setIsin("PL0009999944");
        stock.setTicker("CDR");
        stock.setName("CD Projekt SA");
        stock.setInstrumentType(InstrumentType.STOCK);
        stock.setCurrency("PLN");
        stock.setExchange("WSE");
        stock.setCountry("PL");
        instrumentRepository.save(stock);

        etf = new Instrument();
        etf.setIsin("IE00B4L5Y983");
        etf.setTicker("IWDA");
        etf.setName("iShares Core MSCI World ETF");
        etf.setInstrumentType(InstrumentType.ETF);
        etf.setCurrency("USD");
        etf.setExchange("XETRA");
        etf.setCountry("IE");
        instrumentRepository.save(etf);

        //ppk fund - no ticker
        fund = new Instrument();
        fund.setIsin("PL0009876543");
        fund.setName("NN PPK 2050");
        fund.setInstrumentType(InstrumentType.FUND);
        fund.setCurrency("PLN");
        fund.setCountry("PL");
        instrumentRepository.save(fund);
    }

    //findByIsin
    @Test
    void findByIsin_returnsInstrument_whenExists() {
        Optional<Instrument> result = instrumentRepository.findByIsin("PL0009999944");

        assertThat(result).isPresent();
        assertThat(result.get().getTicker()).isEqualTo("CDR");
        assertThat(result.get().getName()).isEqualTo("CD Projekt SA");
    }

    @Test
    void findByIsin_returnEmpty_whenNotFound() {
        Optional<Instrument> result = instrumentRepository.findByIsin("XX0000000000");

        assertThat(result).isEmpty();
    }

    //findByTicker
    @Test
    void findByTicker_ReturnsInstrument_whenExists() {
        Optional<Instrument> result = instrumentRepository.findByTicker("CDR");

        assertThat(result).isPresent();
        assertThat(result.get().getIsin()).isEqualTo("PL0009999944");
    }

    @Test
    void findByTicker_returnsEmpty_whenTickerIsNull() {
        //Fund has no ticker - should return empty
        Optional<Instrument> result = instrumentRepository.findByTicker("NONEXISTENT");

        assertThat(result).isEmpty();
    }

    //findByInstrumentType
    @Test
    void findByInstrumentType_returnsOnlyMatchingType() {
        List<Instrument> result = instrumentRepository.findByInstrumentType(InstrumentType.STOCK);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTicker()).isEqualTo("CDR");
    }

    @Test
    void findByInstrumentType_returnsEmpty_whenNoneExists() {
        List<Instrument> result = instrumentRepository.findByInstrumentType(InstrumentType.BOND);

        assertThat(result).isEmpty();
    }

    //existsByIsin
    @Test
    void existsByIsin_returnsTrue_whenExists() {
        boolean result = instrumentRepository.existsByIsin("PL0009999944");

        assertThat(result).isTrue();
    }

    @Test
    void existsByIsin_returnsFalse_whenNotExists() {
        boolean result = instrumentRepository.existsByIsin("XX0000000000");

        assertThat(result).isFalse();
    }
}
