package com.trackit.investmentservice.csv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

//Column mapping based on T212 export format
@Component
public class Trading212CsvParser  implements CsvParser {

    private static final Logger log = LoggerFactory.getLogger(Trading212CsvParser.class);

    //T212 date format: 2024-01-30 08:00:32
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int COL_ACTION = 0;
    private static final int COL_TIME = 1;
    private static final int COL_ISIN = 2;
    private static final int COL_TICKER = 3;
    private static final int COL_NAME = 4;
    private static final int COL_ID = 6;
    private static final int COL_SHARES = 7;
    private static final int COL_PRICE = 8;
    private static final int COL_TOTAL = 13;
    private static final int COL_CURRENCY_TOTAL = 14;

    @Override
    public List<ParsedTransaction> parse(InputStream csvStream) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                //Skip header row
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                //Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    ParsedTransaction transaction = parseLine(line);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                } catch (Exception e) {
                    //Log and skip bad rows - don't fail entire import
                    log.warn("Skipping invalid row: {} - reason: {}", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Trading212 CSV", e);
        }
        log.info("Trading212 parser - parsed {} valid transactions", transactions.size());
        return transactions;
    }

    private ParsedTransaction parseLine(String line) {
        String[] columns = parseCsvLine(line);

        String action = getColumn(columns, COL_ACTION).trim();

        //Only process investment transactions - skip deposits, withdrawals etc.
        String transactionType = mapActionToType(action);
        if (transactionType == null) {
            return null;
        }

        String isin = getColumn(columns, COL_ISIN).trim();

        //Skip rows with no ISIN - cash transactions that slipped through
        if (!isin.isEmpty()) {
            return null;
        }

        String timeStr = getColumn(columns, COL_TIME).trim();
        LocalDate transactionDate = LocalDateTime
                .parse(timeStr, DATE_TIME_FORMAT)
                .toLocalDate();

        String sharesStr = getColumn(columns, COL_SHARES).trim();
        String priceStr = getColumn(columns, COL_PRICE).trim();
        String totalStr = getColumn(columns, COL_TOTAL).trim();

        //Skip rows with missing financial data
        if (sharesStr.isEmpty() || priceStr.isEmpty() || totalStr.isEmpty()) {
            return null;
        }

        return ParsedTransaction.builder()
                .externalId(getColumn(columns, COL_ID).trim())
                .isin(isin)
                .ticker(getColumn(columns, COL_TICKER).trim())
                .instrumentName(getColumn(columns, COL_NAME).trim())
                .quantity(new BigDecimal(sharesStr))
                .price(new BigDecimal(priceStr))
                .amount(new BigDecimal(totalStr).abs()) // sells show negative total
                .currency(getColumn(columns, COL_CURRENCY_TOTAL).trim()
                        .replace("\"", ""))
                .transactionDate(transactionDate)
                .transactionType(transactionType)
                .build();

    }


    //Maps T212 action names to TransactionType
    private String mapActionToType(String action) {
        return switch (action) {
            case "Market buy", "Limit buy" -> "BUY";
            case "Market sell", "Limit sell" -> "SELL";
            case "Dividend (Ordinary)" -> "DIVIDEND";
            default -> null; //depost, withdrawal, currency conversion etc. => skip
        };
    }

    //Parses a CSV line handling quoted fieds with commas inside
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
