package com.trackit.bankaccountservice.dto.tink;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

// Response from POST /api/v1/oauth/token — Tink v1 uses snake_case
@Getter
@Setter
public class TinkTokenResponseDTO {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    // Seconds until the access token expires
    @JsonProperty("expires_in")
    private int expiresIn;

    private String scope;
}