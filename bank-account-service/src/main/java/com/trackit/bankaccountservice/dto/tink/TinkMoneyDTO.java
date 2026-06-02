package com.trackit.bankaccountservice.dto.tink;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// Tink v2 monetary value — shared by account balances and transaction amounts
// Actual value = unscaledValue / 10^scale
// e.g. scale=2, unscaledValue=150000 → 1500.00 PLN
// Negative unscaledValue indicates a debit/outbound transaction
@Getter
@Setter
public class TinkMoneyDTO {

    private String currencyCode;
    private Value value;

    @Getter
    @Setter
    public static class Value {
        private int scale;
        // String to safely handle large values without precision loss
        private String unscaledValue;
    }

    public BigDecimal toBigDecimal() {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.getUnscaledValue()).movePointLeft(value.getScale());
    }
}