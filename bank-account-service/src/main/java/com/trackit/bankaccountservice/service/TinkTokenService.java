package com.trackit.bankaccountservice.service;

import com.trackit.bankaccountservice.config.TinkConfig;
import com.trackit.bankaccountservice.dto.tink.TinkTokenResponseDTO;
import com.trackit.bankaccountservice.model.TinkToken;
import com.trackit.bankaccountservice.repository.TinkTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TinkTokenService {

    //Re-obtain before expiry to avoid mid-request failures
    private static final int EXPIRY_SAFETY_MARGIN_MINUTES = 5;

    //Scopes needed for system-level calls: user management, delegation, provider listing
    private static final String CLIENT_SCOPES = "user:create,authorization:grant,providers:read,credentials:read";

    private final TinkTokenRepository tokenRepository;
    private final TinkConfig config;
    private final RestClient tinkRestClient;

    public String getClientAccessToken() {
        return tokenRepository.findFirstBy()
                .filter(t -> !isAboutToExpire(t.getAccessExpiresAt()))
                .map(TinkToken::getAccessToken)
                .orElseGet(() -> obtainAndStore().getAccessToken());
    }

    //Exchanges callback code for a short-lived user token — not stored, used only during sync
    public String getUserAccessToken(String authorizationCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("code", authorizationCode);

        return tinkRestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(TinkTokenResponseDTO.class)
                .getAccessToken();
    }

    private TinkToken obtainAndStore() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("scope", CLIENT_SCOPES);

        TinkTokenResponseDTO response = tinkRestClient.post()
                .uri("/api/v1/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(TinkTokenResponseDTO.class);

        //Upsert — always only one row in tink_tokens
        TinkToken token = tokenRepository.findFirstBy().orElse(new TinkToken());
        token.setAccessToken(response.getAccessToken());
        token.setAccessExpiresAt(LocalDateTime.now().plusSeconds(response.getExpiresIn()));

        return tokenRepository.save(token);
    }

    private boolean isAboutToExpire(LocalDateTime expiresAt) {
        return LocalDateTime.now().plusMinutes(EXPIRY_SAFETY_MARGIN_MINUTES).isAfter(expiresAt);
    }
}