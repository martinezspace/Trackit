package com.trackit.investmentservice.csv;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

//Raw data parsed from a CSV row
//Not JPA entity just a data carrier between parser and ImportService
//ImportService takes this and creates real InvestmentTransaction entities
@Getter
@Builder
public class ParsedTransaction {

    private final String externalId;
    private final String isin;
    private final String ticker;
    private final String instrumentName;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final BigDecimal amount;
    private final String currency;
    private final LocalDate transactionDate;
    private final String transactionType;
}
