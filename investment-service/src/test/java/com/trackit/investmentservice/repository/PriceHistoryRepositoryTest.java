package com.trackit.investmentservice.repository;

import com.trackit.investmentservice.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PriceHistoryRepositoryTest {

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    private Instrument cspx;
    private Instrument sxrv;
    private PriceHistory latestPrice;

    @BeforeEach
    void setUp() {
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

        // Three CSPX prices across different dates
        PriceHistory jan = new PriceHistory();
        jan.setInstrument(cspx);
        jan.setPriceDate(LocalDate.of(2024, 1, 31));
        jan.setClosePrice(new BigDecimal("510.0000"));
        jan.setCurrency("USD");
        jan.setSource("ALPHA_VANTAGE");
        priceHistoryRepository.save(jan);

        PriceHistory feb = new PriceHistory();
        feb.setInstrument(cspx);
        feb.setPriceDate(LocalDate.of(2024, 2, 29));
        feb.setClosePrice(new BigDecimal("515.0000"));
        feb.setCurrency("USD");
        feb.setSource("ALPHA_VANTAGE");
        priceHistoryRepository.save(feb);

        latestPrice = new PriceHistory();
        latestPrice.setInstrument(cspx);
        latestPrice.setPriceDate(LocalDate.of(2024, 3, 15));
        latestPrice.setClosePrice(new BigDecimal("520.7500"));
        latestPrice.setCurrency("USD");
        latestPrice.setSource("ALPHA_VANTAGE");
        priceHistoryRepository.save(latestPrice);

        // One SXRV price
        PriceHistory sxrvPrice = new PriceHistory();
        sxrvPrice.setInstrument(sxrv);
        sxrvPrice.setPriceDate(LocalDate.of(2024, 3, 15));
        sxrvPrice.setClosePrice(new BigDecimal("900.0000"));
        sxrvPrice.setCurrency("USD");
        priceHistoryRepository.save(sxrvPrice);
    }

    // findTopByInstrument_IdOrderByPriceDateDesc
    @Test
    void findTopByInstrument_IdOrderByPriceDateDesc_returnsLatestPrice() {
        Optional<PriceHistory> result = priceHistoryRepository
                .findTopByInstrument_IdOrderByPriceDateDesc(cspx.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getPriceDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(result.get().getClosePrice()).isEqualByComparingTo(new BigDecimal("520.7500"));
    }

    @Test
    void findTopByInstrument_IdOrderByPriceDateDesc_returnsEmpty_whenNoPrices() {
        Instrument noPrices = new Instrument();
        noPrices.setIsin("PL0009999944");
        noPrices.setTicker("CDR");
        noPrices.setName("CD Projekt SA");
        noPrices.setInstrumentType(InstrumentType.STOCK);
        noPrices.setCurrency("PLN");
        instrumentRepository.save(noPrices);

        Optional<PriceHistory> result = priceHistoryRepository
                .findTopByInstrument_IdOrderByPriceDateDesc(noPrices.getId());

        assertThat(result).isEmpty();
    }

    // findByInstrument_IdAndPriceDateBetweenOrderByPriceDateAsc
    @Test
    void findByInstrument_IdAndPriceDateBetween_returnsOnlyPricesInRange() {
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);

        List<PriceHistory> result = priceHistoryRepository
                .findByInstrument_IdAndPriceDateBetweenOrderByPriceDateAsc(
                        cspx.getId(), from, to);

        // Should return Feb and March prices, not January
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPriceDate()).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(result.get(1).getPriceDate()).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    void findByInstrument_IdAndPriceDateBetween_returnsEmpty_whenNoMatchingPrices() {
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);

        List<PriceHistory> result = priceHistoryRepository
                .findByInstrument_IdAndPriceDateBetweenOrderByPriceDateAsc(
                        cspx.getId(), from, to);

        assertThat(result).isEmpty();
    }

    @Test
    void findByInstrument_IdAndPriceDateBetween_returnsOnlyThatInstrumentsPrices() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        List<PriceHistory> result = priceHistoryRepository
                .findByInstrument_IdAndPriceDateBetweenOrderByPriceDateAsc(
                        sxrv.getId(), from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstrument().getTicker()).isEqualTo("SXRV");
    }

    // existsByInstrument_IdAndPriceDate
    @Test
    void existsByInstrument_IdAndPriceDate_returnsTrue_whenExists() {
        boolean result = priceHistoryRepository
                .existsByInstrument_IdAndPriceDate(cspx.getId(), LocalDate.of(2024, 3, 15));

        assertThat(result).isTrue();
    }

    @Test
    void existsByInstrument_IdAndPriceDate_returnsFalse_whenNotExists() {
        boolean result = priceHistoryRepository
                .existsByInstrument_IdAndPriceDate(cspx.getId(), LocalDate.of(2024, 4, 1));

        assertThat(result).isFalse();
    }

    @Test
    void existsByInstrument_IdAndPriceDate_returnsFalse_whenWrongInstrument() {
        // CSPX has a price on 2024-03-15 but SXRV should not match
        boolean result = priceHistoryRepository
                .existsByInstrument_IdAndPriceDate(UUID.randomUUID(), LocalDate.of(2024, 3, 15));

        assertThat(result).isFalse();
    }
}