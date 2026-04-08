package com.trackit.investmentservice.csv;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class NationaleNederlandenPpkParser  implements CsvParser {

    private static final Logger log = LoggerFactory.getLogger(NationaleNederlandenPpkParser.class);

    private static final int DATA_START_ROW = 5;

    private static final int COL_DATE = 0;
    private static final int COL_TYPE = 2;
    private static final int COL_AMOUNT = 4;
    private static final int COL_FUND_NAME = 6;
    private static final int COL_UNIT_PRICE = 7;
    private static final int COL_UNITS = 8;

    @Override
    public List<ParsedTransaction> parse(InputStream xlsStream) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        try (Workbook workbook = new HSSFWorkbook(xlsStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;

                try {
                    ParsedTransaction transaction = parseRow(row);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                } catch (Exception e) {
                    log.warn("Skipping invalid NN PPK row {} - reason: {}", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse National-Nederlanden PPK XLS file", e);
        }
        log.info("Nationale-Nederlanden PPK parser - parsed {} valid transactions", transactions.size());
        return transactions;
    }

    private ParsedTransaction parseRow(Row row) {
        String transactionTypePolish = getCellString(row, COL_TYPE);
        String transactionType = mapTransactionType(transactionTypePolish);

        if (transactionType == null) {
            return null;
        }

        String dateStr = getCellString(row, COL_DATE);
        if (dateStr.isEmpty()) return null;
        LocalDate transactionDate = LocalDate.parse(dateStr.substring(0, 10));

        String fundName = getCellString(row, COL_FUND_NAME).trim();
        if(fundName.isEmpty()) return null;

        BigDecimal amount = getCellDecimal(row, COL_AMOUNT);
        BigDecimal unitPrice = getCellDecimal(row, COL_UNIT_PRICE);
        BigDecimal units = getCellDecimal(row, COL_UNITS);

        if (amount == null || unitPrice == null || units == null) return null;
        if (unitPrice.compareTo(BigDecimal.ZERO) == 0) return null;

        //Build stable external ID for deduplication
        String externalId = buildExternalId(dateStr, transactionTypePolish, fundName, amount);

        return ParsedTransaction.builder()
                .externalId(externalId)
                .isin(null) //PPK funds have no ISIN
                .ticker(null) //PPK fund have no ticker
                .instrumentName(fundName)
                .quantity(units.abs())
                .price(unitPrice)
                .amount(amount.abs())
                .currency("PLN") //PPK is always PLN
                .transactionDate(transactionDate)
                .transactionType(transactionType)
                .build();
    }

    private String mapTransactionType(String type) {
        if (type == null) return null;
        return switch (type.trim()) {
            case "Wpłata podstawowa pracownika",
                 "Wpłata podstawowa pracodawcy",
                 "Dopłata roczna od państwa",
                 "Wpłata dodatkowa pracownika",
                 "Wpłata powitalna" -> "BUY";
            case "Wypłata", "Zwrot" -> "SELL";
            default -> null;
        };
    }

    private String buildExternalId(String date, String type, String fundName, BigDecimal amount) {
        String typeCode = type.replaceAll("[^a-zA-Z]", "");
        typeCode = typeCode.substring(0, Math.min(6, typeCode.length()));
        return "NN_PPK_" + date.substring(0, 10) + "_" + typeCode + "_" + amount.abs().toPlainString();
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
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            default -> "";
        };
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
}
