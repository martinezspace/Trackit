package com.trackit.investmentservice.csv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class Trading212StandardParser implements CsvParser {

    private static final Logger log = LoggerFactory.getLogger(Trading212StandardParser.class);

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Column indices based on Trading212 CSV structure
    private static final int COL_ACTION = 0;
    private static final int COL_TIME = 1;
    private static final int COL_ISIN = 2;
    private static final int COL_TICKER = 3;
    private static final int COL_NAME = 4;
    private static final int COL_ID = 6;
    private static final int COL_SHARES = 7;
    private static final int COL_PRICE = 8;
    // Total column varies by CSV version — handled dynamically below

    @Override
    public List<ParsedTransaction> parse(InputStream csvStream) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {

            log.info("Starting to parse CSV stream");

            String line;
            boolean firstLine = true;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                log.info("Reading line {}: {}", lineCount,
                        line.substring(0, Math.min(50, line.length())));

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    ParsedTransaction transaction = parseLine(line);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                } catch (Exception e) {
                    log.warn("Skipping invalid row: {} — reason: {}",
                            line.substring(0, Math.min(50, line.length())),
                            e.getMessage(), e);
                }
            }

            log.info("Total lines read: {}", lineCount);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Trading212 CSV", e);
        }

        log.info("Trading212 parser — parsed {} valid transactions", transactions.size());
        return transactions;
    }

    private ParsedTransaction parseLine(String line) {
        String[] columns = parseCsvLine(line);

        String action = getColumn(columns, COL_ACTION).trim();
        String cleaned = action.replaceAll("[^\\x20-\\x7E]", "").trim();

        String transactionType = mapActionToType(cleaned);
        if (transactionType == null) {
            return null;
        }

        String isin = getColumn(columns, COL_ISIN).trim();
        if (isin.isEmpty()) {
            return null;
        }

        String timeStr = getColumn(columns, COL_TIME).trim();
        LocalDate transactionDate = LocalDateTime
                .parse(timeStr, DATE_TIME_FORMAT)
                .toLocalDate();

        String sharesStr = getColumn(columns, COL_SHARES).trim();
        String priceStr = getColumn(columns, COL_PRICE).trim();

        if (sharesStr.isEmpty() || priceStr.isEmpty()) {
            return null;
        }

        BigDecimal quantity = new BigDecimal(sharesStr);
        BigDecimal price = new BigDecimal(priceStr);

        // Find Total column dynamically — look for first non-empty numeric value after price
        // Trading212 CSV has different column counts depending on export version
        BigDecimal amount = findAmount(columns, quantity, price);

        // Find currency — look for 3-letter uppercase value after amount
        String currency = findCurrency(columns);
        if (currency.isEmpty()) {
            currency = "EUR"; // fallback
        }

        return ParsedTransaction.builder()
                .externalId(getColumn(columns, COL_ID).trim())
                .isin(isin)
                .ticker(getColumn(columns, COL_TICKER).trim())
                .instrumentName(getColumn(columns, COL_NAME).trim())
                .quantity(quantity)
                .price(price)
                .amount(amount)
                .currency(currency)
                .transactionDate(transactionDate)
                .transactionType(transactionType)
                .build();
    }

    // Find amount — try known column indices first, fall back to quantity × price
    private BigDecimal findAmount(String[] columns, BigDecimal quantity, BigDecimal price) {
        // Try common Total column positions
        int[] totalCandidates = {13, 11, 12, 14};
        for (int idx : totalCandidates) {
            String val = getColumn(columns, idx).trim().replace("\"", "");
            if (!val.isEmpty()) {
                try {
                    BigDecimal amount = new BigDecimal(val);
                    if (amount.compareTo(BigDecimal.ZERO) != 0) {
                        return amount.abs();
                    }
                } catch (NumberFormatException ignored) {
                    // not a number, try next
                }
            }
        }
        // Fall back to quantity × price
        log.debug("Total column not found — calculating from quantity × price");
        return quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    // Find currency — look for 3-letter uppercase ISO code in columns
    private String findCurrency(String[] columns) {
        for (String col : columns) {
            String val = col.trim().replace("\"", "");
            if (val.matches("[A-Z]{3}")) {
                return val;
            }
        }
        return "";
    }

    private String mapActionToType(String action) {
        return switch (action) {
            case "Market buy", "Limit buy" -> "BUY";
            case "Market sell", "Limit sell" -> "SELL";
            case "Dividend (Ordinary)" -> "DIVIDEND";
            default -> null;
        };
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private String getColumn(String[] columns, int index) {
        if (index >= columns.length) {
            return "";
        }
        return columns[index];
    }
}