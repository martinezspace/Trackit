package com.trackit.bankaccountservice.dto.tink;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

// Response from POST /api/v1/oauth/authorization-grant/delegate
// The code is embedded in the Tink Link URL that the user follows to authenticate with their bank
@Getter
@Setter
public class TinkGrantResponseDTO {

    private String code;

    // Seconds until the authorization code expires (typically 300s)
    @JsonProperty("expires_in")
    private int expiresIn;
}