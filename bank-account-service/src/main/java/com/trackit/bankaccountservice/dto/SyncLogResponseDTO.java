package com.trackit.bankaccountservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SyncLogResponseDTO {

    private String id;
    private String connectionId;
    private String accountId;           // null if sync covered all accounts
    private String trigger;             // MANUAL, SCHEDULED, WEBHOOK
    private String status;              // RUNNING, COMPLETED, FAILED
    private int transactionsFetched;
    private int transactionsNew;
    private int transactionsSuggested;
    private String errorMessage;        // null unless status is FAILED
    private String startedAt;
    private String completedAt;         // null while still RUNNING
}