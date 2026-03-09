package com.trackit.investmentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentAccountResponseDTO {
    private String id;
    private String accountType;
    private String brokerName;
    private String displayName;
    private String accountNumber;
    private String currency;
    private boolean active;
    private String notes;
    private String createdAt;
}
