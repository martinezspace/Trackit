package com.trackit.analyticsservice.dto.client.bank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankAccountDTO {

    private String id;
    private String displayName;
    private String currency;
    private String currentBalance;  // null until balance fetched from Tink
    private boolean active;
}