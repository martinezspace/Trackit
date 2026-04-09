package com.trackit.investmentservice.csv;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class XtbStandardParser implements CsvParser {

    private static final Logger log = LoggerFactory.getLogger(XtbStandardParser.class);

    private static final String CASH_OPERATION_SHEET = "Cash Operations";
    private static final int DATA_START_ROW = 5;

    private static final int COL_TYPE = 0;
    private static final int COL_TICKER = 1;
    private static final int COL_INSTRUMENT = 2;
    private static final int COL_TIME = 3;
    private static final int COL_AMOUNT = 4;
    private static final int COL_ID = 5;
    private static final int COL_COMMENT = 6;

    //Matches "OPEN BUY 0.284 @ 457.45" or "OPEN BUY 14/14.3522 @ 457.45"
    //Group 1: quantity (before slash if present, or full quantity)
    //Group 2: total quantity after slash (optional)
    //Group 3: price after @
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "(?:OPEN|CLOSE)\\s+(?:BUY|SELL)\\s+([\\d.]+)(?:/([\\d.]+))?\\s+@\\s+([\\d.]+)"
    );

    @Override
    public List<ParsedTransaction> parse(InputStream xlsxStream) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(xlsxStream)) {
            Sheet sheet = workbook.getSheet(CASH_OPERATION_SHEET);
            if (sheet == null) {
                throw new RuntimeException("Sheet 'Cash Operations' not found in XTB file");
            }

            for (int i = DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;

                try {
                    ParsedTransaction transaction = parseRow(row);
                    if(transaction != null) {
                        transactions.add(transaction);
                    }
                } catch (Exception e) {
                    log.warn("Skipping invalid XTB row {} - reason {}", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XTB XLSX file", e);
        }
        log.info("XTB standard parser - parsed {} valid transactions", transactions.size());
        return transactions;
    }

    private ParsedTransaction parseRow(Row row) {
        String type = getCellString(row, COL_TYPE);
        String transactionType = mapTransactionType(type);
        if (transactionType == null) return null;

        String ticker = getCellString(row, COL_TICKER).trim();
        String instrumentName = getCellString(row, COL_INSTRUMENT).trim();
        String comment = getCellString(row, COL_COMMENT).trim();
        String externalId = getCellString(row, COL_ID).trim();

        if (ticker.isEmpty() || instrumentName.isEmpty()) return null;

        //Parse date from datetime cell
        LocalDate transactionDate = getCellDate(row, COL_TIME);
        if (transactionDate == null) return null;

        //Amount is always present - negative for buys, positive for sells
        BigDecimal amount = getCellDecimal(row, COL_AMOUNT);
        if (amount == null) return null;
        amount = amount.abs();

        //For dividends - no comment to parse, derive quantity from amount/price
        if (transactionType.equals("DIVIDEND")) {
            return ParsedTransaction.builder()
                    .externalId(externalId)
                    .isin(null)
                    .ticker(ticker)
                    .instrumentName(instrumentName)
                    .quantity(BigDecimal.ONE) //dividend quantity not meaningful
                    .price(amount)
                    .amount(amount)
                    .currency("EUR") // XTB EUR account
                    .transactionDate(transactionDate)
                    .transactionType(transactionType)
                    .build();
        }

        //Parse quantity and price from comment
        QuantityPrice qp = parseComment(comment);
        if (qp == null) {
            log.warn("Could not parse comment: {}", comment);
            return null;
        }

        return ParsedTransaction.builder()
                .externalId(externalId)
                .isin(null) //XTB doesnt provide ISIN, enriched later by PriceWorker
                .ticker(ticker)
                .instrumentName(instrumentName)
                .quantity(qp.quantity)
                .price(qp.price)
                .amount(amount)
                .currency("EUR") //XTB EUR account
                .transactionDate(transactionDate)
                .transactionType(transactionType)
                .build();
    }

    //Parse quantity and price from XTB comment field
    //Uses total quantity (after slash) when partial fills are present
    private QuantityPrice parseComment(String comment) {
        if (comment == null || comment.isEmpty()) return null;

        Matcher matcher = COMMENT_PATTERN.matcher(comment);
        if (!matcher.find()) return null;

        String qtyStr = matcher.group(1);
        String priceStr = matcher.group(3);

        BigDecimal quantity = new BigDecimal(qtyStr);
        BigDecimal price = new BigDecimal(priceStr);

        return new QuantityPrice(quantity, price);
    }

    private String mapTransactionType(String type) {
        if (type == null) return null;
        return switch (type.trim()) {
            case "Stock purchase" -> "BUY";
            case "Stock sell" -> "SELL";
            case "Dividend" -> "DIVIDEND";
            default -> null; //skip deposits, withdrawals, taxes etc.
        };
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }

    private LocalDate getCellDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getCellDecimal(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue())
                        .setScale(6, RoundingMode.HALF_UP);
                case STRING -> new BigDecimal(cell.getStringCellValue()
                        .trim().replace(",", "."));
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    //Simple value object for parsed quantity + price from comment
    private record QuantityPrice(BigDecimal quantity, BigDecimal price) {}
}
