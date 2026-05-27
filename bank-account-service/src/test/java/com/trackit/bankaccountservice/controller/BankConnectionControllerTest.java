package com.trackit.bankaccountservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.bankaccountservice.dto.BankConnectionCreateDTO;
import com.trackit.bankaccountservice.dto.BankConnectionResponseDTO;
import com.trackit.bankaccountservice.dto.BankConnectionUpdateDTO;
import com.trackit.bankaccountservice.exception.GlobalExceptionHandler;
import com.trackit.bankaccountservice.exception.ResourceNotFoundException;
import com.trackit.bankaccountservice.model.ConnectionStatus;
import com.trackit.bankaccountservice.service.BankConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest loads only the web layer - controller, filters, validation
// @Import(GlobalExceptionHandler.class) ensures correct status codes match production
@WebMvcTest(BankConnectionController.class)
@Import(GlobalExceptionHandler.class)
public class BankConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BankConnectionService bankConnectionService;

    private UUID userId;
    private UUID connectionId;
    private BankConnectionResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        connectionId = UUID.randomUUID();

        testResponseDTO = new BankConnectionResponseDTO();
        testResponseDTO.setId(connectionId.toString());
        testResponseDTO.setInstitutionId("SANDBOXFINANCE_SFIN0000");
        testResponseDTO.setInstitutionName("Sandbox Finance");
        testResponseDTO.setStatus("ACTIVE");
    }

    // GET /api/bank-connections

    @Test
    public void getAllConnections_returns200_withList() throws Exception {
        when(bankConnectionService.getAllConnectionsForUser(userId))
                .thenReturn(List.of(testResponseDTO));

        mockMvc.perform(get("/api/bank-connections")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].institutionId").value("SANDBOXFINANCE_SFIN0000"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    public void getAllConnections_returns200_withEmptyList_whenUserHasNoConnections() throws Exception {
        when(bankConnectionService.getAllConnectionsForUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/bank-connections")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // GET /api/bank-connections/{id}

    @Test
    public void getConnection_returns200_whenFound() throws Exception {
        when(bankConnectionService.getConnectionById(connectionId, userId))
                .thenReturn(testResponseDTO);

        mockMvc.perform(get("/api/bank-connections/{id}", connectionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionId").value("SANDBOXFINANCE_SFIN0000"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    public void getConnection_returns404_whenConnectionNotFound() throws Exception {
        when(bankConnectionService.getConnectionById(connectionId, userId))
                .thenThrow(new ResourceNotFoundException("Connection not found: " + connectionId));

        mockMvc.perform(get("/api/bank-connections/{id}", connectionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    // POST /api/bank-connections

    @Test
    public void createConnection_returns201_withCreatedDTO() throws Exception {
        BankConnectionCreateDTO createDTO = new BankConnectionCreateDTO();
        createDTO.setInstitutionId("SANDBOXFINANCE_SFIN0000");
        createDTO.setInstitutionName("Sandbox Finance");

        when(bankConnectionService.createConnection(eq(userId), any(BankConnectionCreateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(post("/api/bank-connections")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institutionId").value("SANDBOXFINANCE_SFIN0000"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    public void createConnection_returns422_whenInstitutionIdMissing() throws Exception {
        BankConnectionCreateDTO createDTO = new BankConnectionCreateDTO();
        createDTO.setInstitutionName("Sandbox Finance");
        // institutionId intentionally missing - should fail @NotBlank validation

        mockMvc.perform(post("/api/bank-connections")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.institutionId").exists());
    }

    @Test
    public void createConnection_returns422_whenInstitutionNameMissing() throws Exception {
        BankConnectionCreateDTO createDTO = new BankConnectionCreateDTO();
        createDTO.setInstitutionId("SANDBOXFINANCE_SFIN0000");
        // institutionName intentionally missing - should fail @NotBlank validation

        mockMvc.perform(post("/api/bank-connections")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.institutionName").exists());
    }

    // PATCH /api/bank-connections/{id}

    @Test
    public void updateConnection_returns200_whenValid() throws Exception {
        BankConnectionUpdateDTO updateDTO = new BankConnectionUpdateDTO();
        updateDTO.setStatus(ConnectionStatus.ACTIVE);

        when(bankConnectionService.updateConnection(eq(connectionId), eq(userId), any(BankConnectionUpdateDTO.class)))
                .thenReturn(testResponseDTO);

        mockMvc.perform(patch("/api/bank-connections/{id}", connectionId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    public void updateConnection_returns404_whenNotFound() throws Exception {
        when(bankConnectionService.updateConnection(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Connection not found"));

        mockMvc.perform(patch("/api/bank-connections/{id}", connectionId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // DELETE /api/bank-connections/{id}

    @Test
    public void deleteConnection_returns204_whenSuccessful() throws Exception {
        mockMvc.perform(delete("/api/bank-connections/{id}", connectionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        verify(bankConnectionService, times(1)).deleteConnection(connectionId, userId);
    }

    @Test
    public void deleteConnection_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Connection not found"))
                .when(bankConnectionService).deleteConnection(any(), any());

        mockMvc.perform(delete("/api/bank-connections/{id}", connectionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }
}