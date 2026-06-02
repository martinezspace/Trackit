package com.trackit.bankaccountservice.dto.tink;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Response wrapper from GET /data/v2/accounts
@Getter
@Setter
public class TinkAccountListDTO {

    private List<TinkAccountDTO> accounts;

    // Cursor for the next page — null when no more pages
    private String nextPageToken;
}