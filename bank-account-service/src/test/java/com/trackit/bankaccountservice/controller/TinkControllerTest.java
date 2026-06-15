package com.trackit.bankaccountservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.bankaccountservice.client.TinkClient;
import com.trackit.bankaccountservice.config.TinkConfig;
import com.trackit.bankaccountservice.dto.BankConnectionCreateDTO;
import com.trackit.bankaccountservice.dto.BankConnectionResponseDTO;
import com.trackit.bankaccountservice.dto.tink.TinkGrantResponseDTO;
import com.trackit.bankaccountservice.dto.tink.TinkProviderDTO;
import com.trackit.bankaccountservice.dto.tink.TinkUserResponseDTO;
import com.trackit.bankaccountservice.exception.GlobalExceptionHandler;
import com.trackit.bankaccountservice.model.BankConnection;
import com.trackit.bankaccountservice.model.ConnectionStatus;
import com.trackit.bankaccountservice.model.SyncTrigger;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
import com.trackit.bankaccountservice.service.BankConnectionService;
import com.trackit.bankaccountservice.service.TinkSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TinkController.class)
@Import(GlobalExceptionHandler.class)
public class TinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TinkClient tinkClient;

    @MockitoBean
    private TinkSyncService syncService;

    @MockitoBean
    private BankConnectionService connectionService;

    @MockitoBean
    private BankConnectionRepository connectionRepository;

    @MockitoBean
    private TinkConfig config;

    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    private UUID userId;
    private UUID connectionId;
    private BankConnectionResponseDTO testConnectionResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        connectionId = UUID.randomUUID();

        testConnectionResponse = new BankConnectionResponseDTO();
        testConnectionResponse.setId(connectionId.toString());
        testConnectionResponse.setUserId(userId.toString());
        testConnectionResponse.setInstitutionId("SANDBOXFINANCE_SFIN0000");
        testConnectionResponse.setInstitutionName("Sandbox Finance");
        testConnectionResponse.setStatus("ACTIVE");

        when(config.getWebhookSecret()).thenReturn(WEBHOOK_SECRET);
    }

    // GET /api/tink/institutions

    @Test
    public void getInstitutions_returns200_withProviderList() throws Exception {
        TinkProviderDTO provider = new TinkProviderDTO();
        provider.setName("SANDBOXFINANCE_SFIN0000");
        provider.setDisplayName("Sandbox Finance");
        provider.setStatus("ENABLED");

        when(tinkClient.getProviders("PL")).thenReturn(List.of(provider));

        mockMvc.perform(get("/api/tink/institutions").param("country", "PL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("SANDBOXFINANCE_SFIN0000"))
                .andExpect(jsonPath("$[0].displayName").value("Sandbox Finance"));
    }

    @Test
    public void getInstitutions_returns200_withDefaultCountry_whenParamOmitted() throws Exception {
        when(tinkClient.getProviders("PL")).thenReturn(List.of());

        mockMvc.perform(get("/api/tink/institutions"))
                .andExpect(status().isOk());

        verify(tinkClient).getProviders("PL");
    }

    // POST /api/tink/connect

    @Test
    public void connect_returns200_withLinkUrl_forNewUser() throws Exception {
        BankConnectionCreateDTO createDTO = new BankConnectionCreateDTO();
        createDTO.setInstitutionId("SANDBOXFINANCE_SFIN0000");
        createDTO.setInstitutionName("Sandbox Finance");

        TinkUserResponseDTO tinkUser = new TinkUserResponseDTO();
        tinkUser.setUserId("tink-user-123");

        TinkGrantResponseDTO grant = new TinkGrantResponseDTO();
        grant.setCode("auth-code-123");

        //No existing connection for this user — Tink user must be created
        when(connectionRepository.findFirstByUserId(userId)).thenReturn(Optional.empty());
        when(tinkClient.createUser(userId.toString())).thenReturn(tinkUser);
        when(connectionService.createConnection(eq(userId), any())).thenReturn(testConnectionResponse);
        when(tinkClient.createAuthorizationGrant("tink-user-123")).thenReturn(grant);
        when(tinkClient.buildLinkUrl("auth-code-123", connectionId.toString()))
                .thenReturn("https://link.tink.com/connect?code=auth-code-123");

        mockMvc.perform(post("/api/tink/connect")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionId").value(connectionId.toString()))
                .andExpect(jsonPath("$.linkUrl").value("https://link.tink.com/connect?code=auth-code-123"));
    }

    @Test
    public void connect_reusesExistingTinkUserId_whenUserAlreadyHasConnection() throws Exception {
        BankConnectionCreateDTO createDTO = new BankConnectionCreateDTO();
        createDTO.setInstitutionId("SANDBOXFINANCE_SFIN0000");
        createDTO.setInstitutionName("Sandbox Finance");

        BankConnection existingConnection = new BankConnection();
        existingConnection.setTinkUserId("existing-tink-user");

        TinkGrantResponseDTO grant = new TinkGrantResponseDTO();
        grant.setCode("auth-code-456");

        when(connectionRepository.findFirstByUserId(userId)).thenReturn(Optional.of(existingConnection));
        when(connectionService.createConnection(eq(userId), any())).thenReturn(testConnectionResponse);
        when(tinkClient.createAuthorizationGrant("existing-tink-user")).thenReturn(grant);
        when(tinkClient.buildLinkUrl(any(), any())).thenReturn("https://link.tink.com/connect");

        mockMvc.perform(post("/api/tink/connect")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk());

        //Existing Tink user reused — createUser must not be called
        verify(tinkClient, never()).createUser(any());
    }

    @Test
    public void connect_returns422_whenInstitutionIdMissing() throws Exception {
        BankConnectionCreateDTO createDTO = new BankConnectionCreateDTO();
        createDTO.setInstitutionName("Sandbox Finance");

        mockMvc.perform(post("/api/tink/connect")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.institutionId").exists());
    }

    // GET /api/tink/callback

    @Test
    public void callback_returns200_andTriggersSync_whenValid() throws Exception {
        when(connectionService.activateWithCredentials(connectionId, "cred-123"))
                .thenReturn(testConnectionResponse);

        mockMvc.perform(get("/api/tink/callback")
                        .param("credentialsId", "cred-123")
                        .param("state", connectionId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("connected"))
                .andExpect(jsonPath("$.connectionId").value(connectionId.toString()));

        verify(syncService).syncConnection(connectionId, userId, SyncTrigger.WEBHOOK);
    }

    // POST /api/tink/webhook

    @Test
    public void webhook_returns401_whenSignatureInvalid() throws Exception {
        mockMvc.perform(post("/api/tink/webhook")
                        .header("X-Tink-Signature", "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void webhook_returns200_whenSignatureValid() throws Exception {
        String body = """
                {"context":{"user_id":"u1"},"event":{"type":"unknown","content":{}}}""";

        mockMvc.perform(post("/api/tink/webhook")
                        .header("X-Tink-Signature", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    public void webhook_setsConnectionToError_onCredentialsErrorEvent() throws Exception {
        BankConnection connection = new BankConnection();
        connection.setStatus(ConnectionStatus.ACTIVE);

        String body = """
                {"context":{"user_id":"u1"},"event":{"type":"credentials:error","content":{"credentialsId":"cred-123"}}}""";

        when(connectionRepository.findByCredentialsId("cred-123")).thenReturn(Optional.of(connection));

        mockMvc.perform(post("/api/tink/webhook")
                        .header("X-Tink-Signature", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(connectionRepository).save(argThat(c -> c.getStatus() == ConnectionStatus.ERROR));
    }

    @Test
    public void webhook_setsConnectionToRevoked_onCredentialsDeletedEvent() throws Exception {
        BankConnection connection = new BankConnection();
        connection.setStatus(ConnectionStatus.ACTIVE);

        String body = """
                {"context":{"user_id":"u1"},"event":{"type":"credentials:deleted","content":{"credentialsId":"cred-456"}}}""";

        when(connectionRepository.findByCredentialsId("cred-456")).thenReturn(Optional.of(connection));

        mockMvc.perform(post("/api/tink/webhook")
                        .header("X-Tink-Signature", sign(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(connectionRepository).save(argThat(c -> c.getStatus() == ConnectionStatus.REVOKED));
    }

    // POST /api/tink/sync/{connectionId}

    @Test
    public void sync_returns200_andCallsSyncService() throws Exception {
        mockMvc.perform(post("/api/tink/sync/{connectionId}", connectionId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk());

        verify(syncService).syncConnection(connectionId, userId, SyncTrigger.MANUAL);
    }

    // Helpers

    private String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}