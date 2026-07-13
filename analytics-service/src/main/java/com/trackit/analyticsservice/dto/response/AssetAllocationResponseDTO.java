package com.trackit.analyticsservice.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetAllocationResponseDTO {

    private String id;
    private String userId;
    private String accountId;
    private String accountDisplayName;
    private String accountType;
    private String instrumentType;
    private String currentValue;
    private String weightPct;
    private String currency;
    private String updatedAt;
}