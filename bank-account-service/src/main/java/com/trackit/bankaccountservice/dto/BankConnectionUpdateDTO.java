package com.trackit.bankaccountservice.dto;

import com.trackit.bankaccountservice.model.ConnectionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankConnectionUpdateDTO {

    //All fields nullable - only provided fields are applied in the service
    //Primary use case is status transitions driven by GoCardless webhook events

    //Updated when GoCardless webhook confirms authorization (PENDING => ACTIVE)
    //or when we detect expirt/errors during sync
    private ConnectionStatus status;

    //Received as String from GoCardless webhook
    private String expiresAt;
}
