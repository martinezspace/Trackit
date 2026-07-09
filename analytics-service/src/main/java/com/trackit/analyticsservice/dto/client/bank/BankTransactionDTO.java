package com.trackit.analyticsservice.dto.client.bank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankTransactionDTO {

    private String id;
    private String accountId;
    private String amount;
    private String direction;       // INBOUND or OUTBOUND
    private String currency;
    private String bookingDate;
    private String categoryId;      // null if uncategorized
    private String categoryName;    // null if uncategorized
    private String categoryColor;   // null if uncategorized
}