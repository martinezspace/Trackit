package com.trackit.investmentservice.csv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Trading212StandardParserTest {

    private Trading212StandardParser parser;

    private static final String CSV_HEADER =
            "Action,Time,ISIN,Ticker,Name,Notes,ID,No. of shares,Price / share," +
                    "Currency (Price / share),Exchange rate,Result,Currency (Result)," +
                    "Total,Currency (Total),Currency conversion from amount," +
                    "Currency (Currency conversion from amount),Currency conversion to amount," +
                    "Currency (Currency conversion to amount),Currency conversion fee," +
                    "Currency (Currency conversion fee)\n";

    @BeforeEach
    void setUp() {
        parser = new Trading212StandardParser();
    }

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    // BUY transactions
    @Test
    void parse_returnsMarketBuyTransaction() {
        String csv = CSV_HEADER +
                "Market buy,2024-01-30 08:00:32,IE00B5BMR087,SXR8,iShares Core S&P 500,," +
                "EOF123,1.15,516.75,PLN,1.0,,,600.00,PLN,,,,,3.59,PLN\n";

        List<ParsedTransaction> result = parser.parse(toStream(csv));

        assertThat(result).hasSize(1);
        ParsedTransaction t = result.get(0);
        assertThat(t.getTransactionType()).isEqualTo("BUY");
        assertThat(t.getIsin()).isEqualTo("IE00B5BMR087");
        assertThat(t.getTicker()).isEqualTo("SXR8");
        assertThat(t.getExternalId()).isEqualTo("EOF123");
        assertThat(t.getQuantity()).isEqualByComparingTo(new BigDecimal("1.15"));
        assertThat(t.getPrice()).isEqualByComparingTo(new BigDecimal("516.75"));
        assertThat(t.getCurrency()).isEqualTo("PLN");
    }

    @Test
    void parse_returnsLimitBuyTransaction() {
        String csv = CSV_HEADER +
                "Limit buy,2024-02-01 10:00:00,IE00B5BMR087,SXR8,iShares Core S&P 500,," +
                "EOF456,2.00,500.00,USD,1.0,,,1000.00,PLN,,,,,0.00,PLN\n";

        List<ParsedTransaction> result = parser.parse(toStream(csv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionType()).isEqualTo("BUY");
    }

    // SELL transactions
    @Test
    void parse_returnsMarketSellTransaction() {
        String csv = CSV_HEADER +
                "Market sell,2024-03-15 14:30:00,IE00B5BMR087,SXR8,iShares Core S&P 500,," +
                "EOF789,1.15,550.00,USD,1.0,,,-632.50,PLN,,,,,0.00,PLN\n";

        List<ParsedTransaction> result = parser.parse(toStream(csv));

        assertThat(result).hasSize(1);
        ParsedTransaction t = result.get(0);
        assertThat(t.getTransactionType()).isEqualTo("SELL");
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("632.50")); // abs value
    }

    // DIVIDEND transactions
    @Test
    void parse_returnsDividendTransaction() {
        String csv = CSV_HEADER +
                "Dividend (Ordinary),2024-04-01 09:00:00,IE00B5BMR087,SXR8,iShares Core S&P 500,," +
                "EOF999,0.00,0.00,USD,1.0,,,12.50,PLN,,,,,0.00,PLN\n";

        List<ParsedTransaction> result = parser.parse(toStream(csv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionType()).isEqualTo("DIVIDEND");
    }

    // Skipped rows
    @Test
    void parse_skipsDepositRows() {
        String csv = CSV_HEADER +
                "Deposit,2024-01-01 10:00:00,,,,,DEP001,,,,,,,3000.00,PLN,,,,,\n";

        List<ParsedTransaction> result = parser.parse(toStream(csv));

        assertThat(result).isEmpty();
    }

    @Test
    void parse_skipsInterestOnCashRows() {
        String csv = CSV_HEADER +
                "Interest on cash,2024-01-02 02:00:00,,,,,INT001,,,,,,,0.43,PLN,,,,,\n";

        List<ParsedTransaction> result = parser.parse(toStream(csv));

        assertThat(result).isEmpty();
    }

    @Test
    void parse_skipsRowsWithNoIsin() {
        String csv = CSV_HEADER +
                "Market buy,2024-01-30 08:00:32,,SXR8,iShares Core S&P 500,," +
                "EOF123,1.15,516.75,USD,1.0,,,600.00,PLN,,,,,3.59,PLN\n";

        List<ParsedTransaction> result = parser.parse(toStream(csv));

        assertThat(result).isEmpty();
    }

    // Multiple rows
    @Test
    void parse_returnsMultipleTransactions() {
        String csv = CSV_HEADER +
                "Market buy,2024-01-30 08:00:32,IE00B5BMR087,SXR8,iShares Core S&P 500,," +
                "EOF001,1.15,516.75,USD,1.0,,,600.00,PLN,,,,,0.00,PLN\n" +
                "Interest on cash,2024-01-31 02:00:00,,,,,INT001,,,,,,,0.43,PLN,,,,,\n" +
                "Market buy,2024-02-01 09:00:00,IE00B53SZB19,SXRV,iShares NASDAQ 100,," +
                "EOF002,0.50,900.00,EUR,1.0,,,450.00,PLN,,,,,0.00,PLN\n";

        List<ParsedTransaction> result = parser.parse(toStream(csv));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIsin()).isEqualTo("IE00B5BMR087");
        assertThat(result.get(1).getIsin()).isEqualTo("IE00B53SZB19");
    }

    // Empty file
    @Test
    void parse_returnsEmptyList_whenOnlyHeader() {
        List<ParsedTransaction> result = parser.parse(toStream(CSV_HEADER));
        assertThat(result).isEmpty();
    }

    // Amount fallback — quantity x price when total missing
    @Test
    void parse_calculatesAmountFromQuantityAndPrice_whenTotalMissing() {
        // Only 13 columns — no Total column
        String csv = "Action,Time,ISIN,Ticker,Name,Notes,ID,No. of shares,Price / share," +
                "Currency (Price / share),Exchange rate,Result,Currency (Result)\n" +
                "Market buy,2024-01-30 08:00:32,IE00B5BMR087,SXR8,iShares Core S&P 500,," +
                "EOF123,2.00,500.00,USD,1.0,,\n";

        List<ParsedTransaction> result = parser.parse(toStream(csv));

        assertThat(result).hasSize(1);
        // amount = 2.00 * 500.00 = 1000.00
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}