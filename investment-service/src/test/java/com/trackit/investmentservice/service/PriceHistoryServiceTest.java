package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.PriceHistoryCreateDTO;
import com.trackit.investmentservice.dto.PriceHistoryResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.PriceHistoryMapper;
import com.trackit.investmentservice.model.*;
import com.trackit.investmentservice.repository.HoldingRepository;
import com.trackit.investmentservice.repository.InstrumentRepository;
import com.trackit.investmentservice.repository.PriceHistoryRepository;
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

class PriceHistoryServiceTest {

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private PriceHistoryMapper priceHistoryMapper;

    @InjectMocks
    private PriceHistoryService priceHistoryService;

    private UUID instrumentId;
    private Instrument testInstrument;
    private PriceHistory testPriceHistory;
    private PriceHistoryResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        instrumentId = UUID.randomUUID();

        testInstrument = new Instrument();
        testInstrument.setIsin("IE00B5BMR087");
        testInstrument.setTicker("CSPX");
        testInstrument.setName("iShares Core S&P 500");
        testInstrument.setInstrumentType(InstrumentType.ETF);
        testInstrument.setCurrency("USD");

        testPriceHistory = new PriceHistory();
        testPriceHistory.setInstrument(testInstrument);
        testPriceHistory.setPriceDate(LocalDate.of(2024, 3, 15));
        testPriceHistory.setClosePrice(new BigDecimal("520.7500"));
        testPriceHistory.setCurrency("USD");
        testPriceHistory.setSource("ALPHA_VANTAGE");

        testResponseDTO = new PriceHistoryResponseDTO();
        testResponseDTO.setInstrumentId(instrumentId.toString());
        testResponseDTO.setInstrumentTicker("CSPX");
        testResponseDTO.setPriceDate("2024-03-15");
        testResponseDTO.setClosePrice("520.7500");
        testResponseDTO.setCurrency("USD");
        testResponseDTO.setSource("ALPHA_VANTAGE");
    }

    // getLatestPrice
    @Test
    void getLatestPrice_returnsDTO_whenPriceExists() {
        when(priceHistoryRepository.findTopByInstrument_IdOrderByPriceDateDesc(instrumentId))
                .thenReturn(Optional.of(testPriceHistory));
        when(priceHistoryMapper.toResponseDTO(testPriceHistory))
                .thenReturn(testResponseDTO);

        PriceHistoryResponseDTO result = priceHistoryService.getLatestPrice(instrumentId);

        assertThat(result.getClosePrice()).isEqualTo("520.7500");
        assertThat(result.getInstrumentTicker()).isEqualTo("CSPX");
    }

    @Test
    void getLatestPrice_throwsException_whenNoPriceExists() {
        when(priceHistoryRepository.findTopByInstrument_IdOrderByPriceDateDesc(instrumentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceHistoryService.getLatestPrice(instrumentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No Price History Found For Instrument");
    }

    // getPriceHistory
    @Test
    void getPriceHistory_returnsList_forDateRange() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);

        when(priceHistoryRepository.findByInstrument_IdAndPriceDateBetweenOrderByPriceDateAsc(
                instrumentId, from, to))
                .thenReturn(List.of(testPriceHistory));
        when(priceHistoryMapper.toResponseDTO(testPriceHistory))
                .thenReturn(testResponseDTO);

        List<PriceHistoryResponseDTO> result = priceHistoryService.getPriceHistory(instrumentId, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPriceDate()).isEqualTo("2024-03-15");
    }

    @Test
    void getPriceHistory_returnsEmptyList_whenNoPricesInRange() {
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);

        when(priceHistoryRepository.findByInstrument_IdAndPriceDateBetweenOrderByPriceDateAsc(
                instrumentId, from, to))
                .thenReturn(List.of());

        List<PriceHistoryResponseDTO> result = priceHistoryService.getPriceHistory(instrumentId, from, to);

        assertThat(result).isEmpty();
    }

    // savePrice
    @Test
    void savePrice_savesAndUpdatesHoldings_whenPriceDoesNotExist() {
        PriceHistoryCreateDTO createDTO = new PriceHistoryCreateDTO();
        createDTO.setInstrumentId(instrumentId);
        createDTO.setPriceDate(LocalDate.of(2024, 3, 15));
        createDTO.setClosePrice(new BigDecimal("520.7500"));
        createDTO.setCurrency("USD");
        createDTO.setSource("ALPHA_VANTAGE");

        // Build a holding to verify it gets updated
        InvestmentAccount account = new InvestmentAccount();
        account.setCurrency("PLN");
        Holding holding = new Holding();
        holding.setAccount(account);
        holding.setInstrument(testInstrument);
        holding.setQuantity(new BigDecimal("2.000000"));
        holding.setTotalInvested(new BigDecimal("1000.00"));
        holding.setCurrency("PLN");

        when(instrumentRepository.findById(instrumentId))
                .thenReturn(Optional.of(testInstrument));
        when(priceHistoryRepository.existsByInstrument_IdAndPriceDate(
                instrumentId, createDTO.getPriceDate()))
                .thenReturn(false);
        when(priceHistoryMapper.toEntity(createDTO, testInstrument))
                .thenReturn(testPriceHistory);
        when(priceHistoryRepository.save(testPriceHistory))
                .thenReturn(testPriceHistory);
        when(holdingRepository.findByInstrument_Id(any()))
                .thenReturn(List.of(holding));
        when(priceHistoryMapper.toResponseDTO(testPriceHistory))
                .thenReturn(testResponseDTO);

        PriceHistoryResponseDTO result = priceHistoryService.savePrice(createDTO);

        assertThat(result.getClosePrice()).isEqualTo("520.7500");
        // Verify holding was updated with new price
        assertThat(holding.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("520.7500"));
        assertThat(holding.getCurrentValue()).isEqualByComparingTo(new BigDecimal("1041.50"));
        verify(holdingRepository, times(1)).saveAll(List.of(holding));
    }

    @Test
    void savePrice_skipsInsert_whenPriceAlreadyExists() {
        PriceHistoryCreateDTO createDTO = new PriceHistoryCreateDTO();
        createDTO.setInstrumentId(instrumentId);
        createDTO.setPriceDate(LocalDate.of(2024, 3, 15));
        createDTO.setClosePrice(new BigDecimal("520.7500"));
        createDTO.setCurrency("USD");

        when(instrumentRepository.findById(instrumentId))
                .thenReturn(Optional.of(testInstrument));
        when(priceHistoryRepository.existsByInstrument_IdAndPriceDate(
                instrumentId, createDTO.getPriceDate()))
                .thenReturn(true);
        when(priceHistoryRepository.findTopByInstrument_IdOrderByPriceDateDesc(instrumentId))
                .thenReturn(Optional.of(testPriceHistory));
        when(priceHistoryMapper.toResponseDTO(testPriceHistory))
                .thenReturn(testResponseDTO);

        priceHistoryService.savePrice(createDTO);

        // Verify save was never called — duplicate skipped
        verify(priceHistoryRepository, never()).save(any());
        verify(holdingRepository, never()).saveAll(any());
    }

    @Test
    void savePrice_throwsException_whenInstrumentNotFound() {
        PriceHistoryCreateDTO createDTO = new PriceHistoryCreateDTO();
        createDTO.setInstrumentId(instrumentId);
        createDTO.setPriceDate(LocalDate.of(2024, 3, 15));
        createDTO.setClosePrice(new BigDecimal("520.7500"));
        createDTO.setCurrency("USD");

        when(instrumentRepository.findById(instrumentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceHistoryService.savePrice(createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Instrument Not Found");
    }
}