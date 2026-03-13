package com.trackit.investmentservice.service;

import com.trackit.investmentservice.dto.InstrumentCreateDTO;
import com.trackit.investmentservice.dto.InstrumentResponseDTO;
import com.trackit.investmentservice.exception.ResourceNotFoundException;
import com.trackit.investmentservice.mapper.InstrumentMapper;
import com.trackit.investmentservice.model.Instrument;
import com.trackit.investmentservice.model.InstrumentType;
import com.trackit.investmentservice.repository.InstrumentRepository;
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

public class InstrumentServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentMapper instrumentMapper;

    @InjectMocks
    private InstrumentService instrumentService;

    private UUID instrumentId;
    private Instrument testInstrument;
    private InstrumentResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        instrumentId = UUID.randomUUID();

        testInstrument = new Instrument();
        testInstrument.setIsin("PL0009999944");
        testInstrument.setTicker("CDR");
        testInstrument.setName("CD Projekt SA");
        testInstrument.setInstrumentType(InstrumentType.STOCK);
        testInstrument.setCurrency("PLN");
        testInstrument.setExchange("WSE");
        testInstrument.setCountry("PL");

        testResponseDTO = new InstrumentResponseDTO();
        testResponseDTO.setId(instrumentId.toString());
        testResponseDTO.setIsin("PL0009999944");
        testResponseDTO.setTicker("CDR");
        testResponseDTO.setName("CD Projekt SA");
        testResponseDTO.setInstrumentType("STOCK");
        testResponseDTO.setCurrency("PLN");
    }

    //getById
    @Test
    void getById_returnsDTO_whenInstrumentExists() {
        when(instrumentRepository.findById(instrumentId))
                .thenReturn(Optional.of(testInstrument));
        when(instrumentMapper.toResponseDTO(testInstrument))
                .thenReturn(testResponseDTO);

        InstrumentResponseDTO result = instrumentService.getById(instrumentId);

        assertThat(result.getIsin()).isEqualTo("PL0009999944");
        assertThat(result.getTicker()).isEqualTo("CDR");
    }

    @Test
    void getById_throwsException_whenNotFound() {
        when(instrumentRepository.findById(instrumentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> instrumentService.getById(instrumentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Instrument Not Found");
    }

    //getByIsin
    @Test
    void getByIsin_returnsDTO_whenInstrumentExists() {
        when(instrumentRepository.findByIsin("PL0009999944"))
                .thenReturn(Optional.of(testInstrument));
        when(instrumentMapper.toResponseDTO(testInstrument))
                .thenReturn(testResponseDTO);

        InstrumentResponseDTO result = instrumentService.getByIsin("PL0009999944");

        assertThat(result.getName()).isEqualTo("CD Projekt SA");
    }

    @Test
    void getByIsin_throwsException_whenNotFound() {
        when(instrumentRepository.findByIsin("PL0009999944"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> instrumentService.getByIsin("PL0009999944"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Instrument Not Found for ISIN");
    }

    //getByType
    @Test
    void getByType_returnsFilteredList() {
        when(instrumentRepository.findByInstrumentType(InstrumentType.STOCK))
                .thenReturn(List.of(testInstrument));
        when(instrumentMapper.toResponseDTO(testInstrument))
                .thenReturn(testResponseDTO);

        List<InstrumentResponseDTO> result = instrumentService.getByType(InstrumentType.STOCK);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstrumentType()).isEqualTo("STOCK");
    }

    @Test
    void getByType_returnsEmptyList_whenNoneFound() {
        when(instrumentRepository.findByInstrumentType(InstrumentType.BOND))
                .thenReturn(List.of());

        List<InstrumentResponseDTO> result = instrumentService.getByType(InstrumentType.BOND);

        assertThat(result).isEmpty();
    }

    //createInstrument
    @Test
    void createInstrument_savesAndReturnsDTO() {
        InstrumentCreateDTO createDTO = new InstrumentCreateDTO();
        createDTO.setIsin("PL0009999944");
        createDTO.setName("CD Projekt SA");
        createDTO.setInstrumentType(InstrumentType.STOCK);
        createDTO.setCurrency("PLN");

        when(instrumentRepository.existsByIsin("PL0009999944"))
                .thenReturn(false);
        when(instrumentMapper.toEntity(createDTO))
                .thenReturn(testInstrument);
        when(instrumentRepository.save(testInstrument))
                .thenReturn(testInstrument);
        when(instrumentMapper.toResponseDTO(testInstrument))
                .thenReturn(testResponseDTO);

        InstrumentResponseDTO result = instrumentService.createInstrument(createDTO);

        assertThat(result.getIsin()).isEqualTo("PL0009999944");
        verify(instrumentRepository, times(1)).save(testInstrument);
    }

    @Test
    void createInstrument_throwsException_whenIsinAlreadyExists() {
        InstrumentCreateDTO createDTO = new InstrumentCreateDTO();
        createDTO.setIsin("PL0009999944");

        when(instrumentRepository.existsByIsin("PL0009999944"))
                .thenReturn(true);

        assertThatThrownBy(() -> instrumentService.createInstrument(createDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Instrument already exists with ISIN");

        verify(instrumentRepository, never()).save(any());
    }

    //findOrCreate
    @Test
    void findOrCreate_returnsExisting_whenIsinAlreadyExists() {
        InstrumentCreateDTO createDTO = new InstrumentCreateDTO();
        createDTO.setIsin("PL0009999944");

        when(instrumentRepository.findByIsin("PL0009999944"))
                .thenReturn(Optional.of(testInstrument));
        when(instrumentMapper.toResponseDTO(testInstrument))
                .thenReturn(testResponseDTO);

        InstrumentResponseDTO result = instrumentService.findOrCreate(createDTO);

        assertThat(result.getIsin()).isEqualTo("PL0009999944");
        //verify save was never called - existing instrument was returned
        verify(instrumentRepository, never()).save(any());
    }

    @Test
    void findOrCreate_createsNew_whenIsinDoesNotExist() {
        InstrumentCreateDTO createDTO = new InstrumentCreateDTO();
        createDTO.setIsin("PL0009999944");
        createDTO.setName("CD Projekt SA");
        createDTO.setInstrumentType(InstrumentType.STOCK);
        createDTO.setCurrency("PLN");

        when(instrumentRepository.findByIsin("PL0009999944"))
                .thenReturn(Optional.empty());
        when(instrumentRepository.existsByIsin("PL0009999944"))
                .thenReturn(false);
        when(instrumentMapper.toEntity(createDTO))
                .thenReturn(testInstrument);
        when(instrumentRepository.save(testInstrument))
                .thenReturn(testInstrument);
        when(instrumentMapper.toResponseDTO(testInstrument))
                .thenReturn(testResponseDTO);

        InstrumentResponseDTO result = instrumentService.findOrCreate(createDTO);

        assertThat(result.getIsin()).isEqualTo("PL0009999944");
        verify(instrumentRepository, times(1)).save(testInstrument);
    }
}
