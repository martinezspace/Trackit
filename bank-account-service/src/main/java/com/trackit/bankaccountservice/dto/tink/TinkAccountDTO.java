package com.trackit.bankaccountservice.dto.tink;

import lombok.Getter;
import lombok.Setter;

// One account from GET /data/v2/accounts — Tink v2 uses camelCase JSON
@Getter
@Setter
public class TinkAccountDTO {

    private String id;
    private String name;

    // CHECKING, SAVINGS, CREDIT_CARD, INVESTMENT, LOAN, OTHER
    private String type;

    private Balance balance;
    private Identifiers identifiers;
    private FinancialInstitution financialInstitution;

    @Getter
    @Setter
    public static class Balance {
        private BalanceEntry bookedBalance;
        private BalanceEntry availableBalance;

        @Getter
        @Setter
        public static class BalanceEntry {
            private TinkMoneyDTO amount;
        }
    }

    @Getter
    @Setter
    public static class Identifiers {
        private Iban iban;

        @Getter
        @Setter
        public static class Iban {
            private String iban;
            private String bban;
        }
    }

    @Getter
    @Setter
    public static class FinancialInstitution {
        private String id;
        private String name;
    }
}