package com.trackit.bankaccountservice.dto;

import com.trackit.bankaccountservice.model.ConnectionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankConnectionUpdateDTO {

    //All fields nullable - only provided fields are applied in the service
    //Primary use case: status transitions driven by Tink webhook events

    //Transitions: PENDING => ACTIVE (webhook), ACTIVE => EXPIRED/REVOKED/ERROR (sync or webhook)
    private ConnectionStatus status;

    private String expiresAt;
}
