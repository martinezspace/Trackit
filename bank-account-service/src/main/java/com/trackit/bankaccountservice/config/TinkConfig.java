package com.trackit.bankaccountservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "tink")
public class TinkConfig {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    // Full callback URL — set per environment via env var
    // Local:  http://localhost:8080/api/tink/callback
    // Prod:   https://yourdomain.com/api/tink/callback
    @NotBlank
    private String redirectUri;

    // Used to verify HMAC-SHA256 signature on incoming Tink webhook requests
    @NotBlank
    private String webhookSecret;

    @Min(1)
    private int syncIntervalHours = 6;
}