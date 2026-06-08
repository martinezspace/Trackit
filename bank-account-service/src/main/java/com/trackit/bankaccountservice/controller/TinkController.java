package com.trackit.bankaccountservice.controller;

import com.trackit.bankaccountservice.client.TinkClient;
import com.trackit.bankaccountservice.config.TinkConfig;
import com.trackit.bankaccountservice.dto.BankConnectionCreateDTO;
import com.trackit.bankaccountservice.dto.BankConnectionResponseDTO;
import com.trackit.bankaccountservice.dto.tink.TinkConnectResponseDTO;
import com.trackit.bankaccountservice.dto.tink.TinkGrantResponseDTO;
import com.trackit.bankaccountservice.dto.tink.TinkProviderDTO;
import com.trackit.bankaccountservice.dto.tink.TinkWebhookEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.bankaccountservice.model.ConnectionStatus;
import com.trackit.bankaccountservice.model.SyncTrigger;
import com.trackit.bankaccountservice.repository.BankConnectionRepository;
import com.trackit.bankaccountservice.service.BankConnectionService;
import com.trackit.bankaccountservice.service.TinkSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tink")
@Tag(name = "Tink", description = "Tink Open Banking — bank linking and data sync")
public class TinkController {

    private final TinkClient tinkClient;
    private final TinkSyncService syncService;
    private final BankConnectionService connectionService;
    private final BankConnectionRepository connectionRepository;
    private final TinkConfig config;
    private final ObjectMapper objectMapper;

    //GET /api/tink/institutions
    @GetMapping("/institutions")
    @Operation(summary = "List available banks for a given country")
    public ResponseEntity<List<TinkProviderDTO>> getInstitutions(
            @RequestParam(defaultValue = "PL") String country) {
        return ResponseEntity.ok(tinkClient.getProviders(country));
    }

    //POST /api/tink/connect
    @PostMapping("/connect")
    @Operation(summary = "Initiate bank connection — returns Tink Link URL for the user to visit")
    public ResponseEntity<TinkConnectResponseDTO> connect(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody BankConnectionCreateDTO request) {

        //Reuse existing Tink user if one already exists for this user
        String tinkUserId = connectionRepository.findFirstByUserId(userId)
                .map(c -> c.getTinkUserId())
                .filter(id -> id != null && !id.isBlank())
                .orElseGet(() -> tinkClient.createUser(userId.toString()).getUserId());

        BankConnectionResponseDTO connection = connectionService.createConnection(userId, request);
        UUID connectionId = UUID.fromString(connection.getId());

        connectionService.setTinkUserId(connectionId, userId, tinkUserId);

        TinkGrantResponseDTO grant = tinkClient.createAuthorizationGrant(tinkUserId);
        String linkUrl = tinkClient.buildLinkUrl(grant.getCode(), connectionId.toString());

        TinkConnectResponseDTO response = new TinkConnectResponseDTO();
        response.setConnectionId(connection.getId());
        response.setLinkUrl(linkUrl);

        return ResponseEntity.ok(response);
    }

    //GET /api/tink/callback
    //Tink redirects here after the user authenticates with their bank
    @GetMapping("/callback")
    @Operation(summary = "Tink OAuth callback — activates connection and triggers initial sync")
    public ResponseEntity<Map<String, String>> callback(
            @RequestParam String code,
            @RequestParam String credentialsId,
            @RequestParam String state) {

        UUID connectionId = UUID.fromString(state);

        BankConnectionResponseDTO connection = connectionService.activateWithCredentials(connectionId, credentialsId);
        UUID userId = UUID.fromString(connection.getUserId());

        syncService.syncConnection(connectionId, userId, SyncTrigger.WEBHOOK);

        return ResponseEntity.ok(Map.of(
                "status", "connected",
                "connectionId", connectionId.toString()
        ));
    }

    //POST /api/tink/webhook
    //Tink sends signed events here for async status changes (credentials error, expiry etc.)
    @PostMapping("/webhook")
    @Operation(summary = "Receive Tink webhook events")
    public ResponseEntity<Void> webhook(
            @RequestHeader("X-Tink-Signature") String signature,
            @RequestBody String rawBody) {

        if (!isValidSignature(rawBody, signature)) {
            log.warn("Received webhook with invalid signature");
            return ResponseEntity.status(401).build();
        }

        try {
            TinkWebhookEventDTO event = objectMapper.readValue(rawBody, TinkWebhookEventDTO.class);
            if (event.getEvent() == null) return ResponseEntity.ok().build();

            String type = event.getEvent().getType();
            Map<String, String> content = event.getEvent().getContent();

            switch (type != null ? type : "") {
                case "credentials:error" -> {
                    String credentialsId = content != null ? content.get("credentialsId") : null;
                    if (credentialsId != null) {
                        connectionRepository.findByCredentialsId(credentialsId).ifPresent(conn -> {
                            conn.setStatus(ConnectionStatus.ERROR);
                            connectionRepository.save(conn);
                        });
                    }
                }
                case "credentials:deleted" -> {
                    String credentialsId = content != null ? content.get("credentialsId") : null;
                    if (credentialsId != null) {
                        connectionRepository.findByCredentialsId(credentialsId).ifPresent(conn -> {
                            conn.setStatus(ConnectionStatus.REVOKED);
                            connectionRepository.save(conn);
                        });
                    }
                }
                default -> log.info("Unhandled Tink webhook event type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to parse webhook body: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    //POST /api/tink/sync/{connectionId}
    @PostMapping("/sync/{connectionId}")
    @Operation(summary = "Manually trigger a sync for a specific connection")
    public ResponseEntity<Void> sync(
            @PathVariable UUID connectionId,
            @RequestHeader("X-User-Id") UUID userId) {
        syncService.syncConnection(connectionId, userId, SyncTrigger.MANUAL);
        return ResponseEntity.ok().build();
    }

    private boolean isValidSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    config.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(computed).equals(signature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}