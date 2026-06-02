package com.trackit.bankaccountservice.dto.tink;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

// Response from POST /api/v1/user/create
// user_id is Tink's internal identifier — stored as tinkUserId on BankConnection
@Getter
@Setter
public class TinkUserResponseDTO {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("external_user_id")
    private String externalUserId;
}