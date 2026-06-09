package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.dto.BankConnectionCreateDTO;
import com.trackit.bankaccountservice.dto.BankConnectionResponseDTO;
import com.trackit.bankaccountservice.dto.BankConnectionUpdateDTO;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.mapper.BankConnectionMapper;
import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.model.ConnectionStatus;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
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

public class BankConnectionServiceTest {

    @Mock
    private BankConnectionRepository bankConnectionRepository;

    @Mock
    private BankConnectionMapper bankConnectionMapper;

    @InjectMocks
    private BankConnectionService bankConnectionService;

    private UUID userId;
    private UUID connectionId;
    private BankConnection testConnection;
    private BankConnectionResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        connectionId = UUID.randomUUID();

        // Build reusable test entity
        testConnection = new BankConnection();
        testConnection.setUserId(userId);
        testConnection.setInstitutionId("MONZO_MONZGB2L");
        testConnection.setInstitutionName("Monzo");
        testConnection.setCredentialsId("cred-monzo-001");
        testConnection.setStatus(ConnectionStatus.ACTIVE);

        // Build reusable test response DTO
        testResponseDTO = new BankConnectionResponseDTO();
        testResponseDTO.setId(connectionId.toString());
        testResponseDTO.setInstitutionId("MONZO_MONZGB2L");
        testResponseDTO.setInstitutionName("Monzo");
        testResponseDTO.setStatus("ACTIVE");
    }

    // getAllConnectionsForUser

    @Test
    public void getAllConnectionsForUser_returnsListOfDTOs() {
        when(bankConnectionRepository.findByUserId(userId))
                .thenReturn(List.of(testConnection));
        when(bankConnectionMapper.toResponseDTO(testConnection))
                .thenReturn(testResponseDTO);

        List<BankConnectionResponseDTO> result = bankConnectionService.getAllConnectionsForUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstitutionId()).isEqualTo("MONZO_MONZGB2L");
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    public void getAllConnectionsForUser_returnsEmptyList_whenUserHasNoConnections() {
        when(bankConnectionRepository.findByUserId(userId)).thenReturn(List.of());

        List<BankConnectionResponseDTO> result = bankConnectionService.getAllConnectionsForUser(userId);

        assertThat(result).isEmpty();
    }

    // getConnectionById

    @Test
    public void getConnectionById_returnsDTO_whenConnectionExists() {
        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.of(testConnection));
        when(bankConnectionMapper.toResponseDTO(testConnection))
                .thenReturn(testResponseDTO);

        BankConnectionResponseDTO result = bankConnectionService.getConnectionById(connectionId, userId);

        assertThat(result.getInstitutionId()).isEqualTo("MONZO_MONZGB2L");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    public void getConnectionById_throwsException_whenConnectionNotFound() {
        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankConnectionService.getConnectionById(connectionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Connection not found");
    }

    @Test
    public void getConnectionById_throwsException_whenConnectionBelongsToDifferentUser() {
        UUID differentUserId = UUID.randomUUID();
        when(bankConnectionRepository.findByIdAndUserId(connectionId, differentUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankConnectionService.getConnectionById(connectionId, differentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Connection not found");
    }

    // createConnection

    @Test
    public void createConnection_savesAndReturnsDTO() {
        BankConnectionCreateDTO createDTO = new BankConnectionCreateDTO();
        createDTO.setInstitutionId("MONZO_MONZGB2L");
        createDTO.setInstitutionName("Monzo");

        when(bankConnectionMapper.toEntity(createDTO, userId)).thenReturn(testConnection);
        when(bankConnectionRepository.save(testConnection)).thenReturn(testConnection);
        when(bankConnectionMapper.toResponseDTO(testConnection)).thenReturn(testResponseDTO);

        BankConnectionResponseDTO result = bankConnectionService.createConnection(userId, createDTO);

        assertThat(result.getInstitutionId()).isEqualTo("MONZO_MONZGB2L");
        verify(bankConnectionRepository, times(1)).save(testConnection);
    }

    @Test
    public void createConnection_passesUserIdToMapper() {
        BankConnectionCreateDTO createDTO = new BankConnectionCreateDTO();
        createDTO.setInstitutionId("MONZO_MONZGB2L");
        createDTO.setInstitutionName("Monzo");

        when(bankConnectionMapper.toEntity(createDTO, userId)).thenReturn(testConnection);
        when(bankConnectionRepository.save(testConnection)).thenReturn(testConnection);
        when(bankConnectionMapper.toResponseDTO(testConnection)).thenReturn(testResponseDTO);

        bankConnectionService.createConnection(userId, createDTO);

        // userId comes from header not body - verify it reaches the mapper correctly
        verify(bankConnectionMapper, times(1)).toEntity(createDTO, userId);
    }

    // updateConnection

    @Test
    public void updateConnection_appliesChangesAndReturnsDTO() {
        BankConnectionUpdateDTO updateDTO = new BankConnectionUpdateDTO();
        updateDTO.setStatus(ConnectionStatus.ACTIVE);

        BankConnectionResponseDTO updatedResponse = new BankConnectionResponseDTO();
        updatedResponse.setStatus("ACTIVE");

        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.of(testConnection));
        when(bankConnectionMapper.applyUpdate(testConnection, updateDTO))
                .thenReturn(testConnection);
        when(bankConnectionRepository.save(testConnection)).thenReturn(testConnection);
        when(bankConnectionMapper.toResponseDTO(testConnection)).thenReturn(updatedResponse);

        BankConnectionResponseDTO result = bankConnectionService.updateConnection(connectionId, userId, updateDTO);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(bankConnectionMapper, times(1)).applyUpdate(testConnection, updateDTO);
        verify(bankConnectionRepository, times(1)).save(testConnection);
    }

    @Test
    public void updateConnection_throwsException_whenConnectionNotFound() {
        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankConnectionService.updateConnection(connectionId, userId, new BankConnectionUpdateDTO()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Connection not found");

        verify(bankConnectionRepository, never()).save(any());
    }

    // deleteConnection

    @Test
    public void deleteConnection_deletesWhenFound() {
        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.of(testConnection));

        bankConnectionService.deleteConnection(connectionId, userId);

        verify(bankConnectionRepository, times(1)).delete(testConnection);
    }

    @Test
    public void deleteConnection_throwsException_whenConnectionNotFound() {
        when(bankConnectionRepository.findByIdAndUserId(connectionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankConnectionService.deleteConnection(connectionId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Connection not found");

        verify(bankConnectionRepository, never()).delete(any());
    }
}