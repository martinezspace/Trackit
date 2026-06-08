package com.trackit.bankaccountservice.dto.tink;

import lombok.Getter;
import lombok.Setter;

// Returned from POST /api/tink/connect — client uses linkUrl to redirect the user to Tink
@Getter
@Setter
public class TinkConnectResponseDTO {

    private String connectionId;
    private String linkUrl;
}