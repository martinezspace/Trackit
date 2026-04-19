package com.trackit.bankaccountservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankConnectionResponseDTO {

    //All fields serialized as String for stable, front-end safe contract
    private String id;
    private String userId;
    private String institutionId;
    private String institutionName;
    private String requisitionId;
    private String status;
    private String expiresAt;       //null if not yet set by provider
    private String lastSyncedAt;    //null if never synced
    private String createdAt;
    private String updatedAt;
}
