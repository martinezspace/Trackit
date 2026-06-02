package com.trackit.bankaccountservice.dto.tink;

import lombok.Getter;
import lombok.Setter;

// One transaction from GET /data/v2/transactions — Tink v2 uses camelCase JSON
// Negative amount.value.unscaledValue = OUTBOUND, positive = INBOUND
@Getter
@Setter
public class TinkTransactionDTO {

    private String id;
    private String accountId;

    // BOOKED or PENDING — we only import BOOKED transactions
    private String status;

    private TinkMoneyDTO amount;

    private Dates dates;
    private Descriptions descriptions;
    private MerchantInformation merchantInformation;
    private Identifiers identifiers;

    @Getter
    @Setter
    public static class Dates {
        // ISO-8601 date strings (yyyy-MM-dd)
        private String booked;
        private String value;
    }

    @Getter
    @Setter
    public static class Descriptions {
        private String original;
        private String display;
    }

    @Getter
    @Setter
    public static class MerchantInformation {
        private String merchantName;
        private String merchantCategoryCode;
    }

    @Getter
    @Setter
    public static class Identifiers {
        // Tink's stable ID for deduplication across syncs
        private String providerTransactionId;
    }
}