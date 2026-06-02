package com.trackit.bankaccountservice.dto.tink;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Response wrapper from GET /data/v2/transactions
// Tink uses cursor-based pagination — keep fetching until nextPageToken is null
@Getter
@Setter
public class TinkTransactionListDTO {

    private List<TinkTransactionDTO> transactions;

    // Cursor for the next page — null when all transactions have been fetched
    private String nextPageToken;
}