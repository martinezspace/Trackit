package com.trackit.analyticsservice.dto.client.investment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentAccountDTO {

    private String id;
    private String accountType;
    private String displayName;
    private String currency;
    private boolean active;
}