package com.trackit.bankaccountservice.dto.tink;

import lombok.Getter;
import lombok.Setter;

// One entry from GET /api/v1/providers/{market}
// name is Tink's provider identifier — used as institutionId when initiating a connection
@Getter
@Setter
public class TinkProviderDTO {

    // Tink's provider identifier — used as institutionId in our BankConnection
    private String name;

    // Human-readable bank name shown to the user
    private String displayName;

    // BANK, CREDIT_CARD, BROKER, etc.
    private String type;

    // ENABLED, DISABLED, TEMPORARY_DISABLED
    private String status;

    private String market;
    private String currency;

    private Images images;

    @Getter
    @Setter
    public static class Images {
        private String icon;
        private String banner;
    }
}