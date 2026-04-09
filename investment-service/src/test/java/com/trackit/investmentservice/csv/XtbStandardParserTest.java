package com.trackit.investmentservice.csv;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XtbStandardParserTest {

    private XtbStandardParser parser;

    @BeforeEach
    void setUp() {
        parser = new XtbStandardParser();
    }

    // Helper — builds in-memory .xlsx matching XTB Cash Operations structure
    private InputStream buildXlsx(Object[][] dataRows) throws Exception {
        Workbook workbook = new XSSFWorkbook();

        // Sheet 1: Closed Positions (ignored by parser)
        workbook.createSheet("Closed Positions");

        // Sheet 2: Cash Operations
        Sheet sheet = workbook.createSheet("Cash Operations");

        // Create date cell style — required so DateUtil.isCellDateFormatted() returns true
        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(
                createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

        // Rows 0-3: metadata
        for (int i = 0; i < 4; i++) {
            sheet.createRow(i);
        }

        // Row 4: headers
        Row header = sheet.createRow(4);
        header.createCell(0).setCellValue("Type");
        header.createCell(1).setCellValue("Ticker");
        header.createCell(2).setCellValue("Instrument");
        header.createCell(3).setCellValue("Time");
        header.createCell(4).setCellValue("Amount");
        header.createCell(5).setCellValue("ID");
        header.createCell(6).setCellValue("Comment");
        header.createCell(7).setCellValue("Product");

        // Data rows starting at row 5
        for (int i = 0; i < dataRows.length; i++) {
            Row row = sheet.createRow(5 + i);
            Object[] data = dataRows[i];
            row.createCell(0).setCellValue((String) data[0]); // type
            row.createCell(1).setCellValue((String) data[1]); // ticker
            row.createCell(2).setCellValue((String) data[2]); // instrument

            // Date cell — must have date style so DateUtil recognises it
            Cell dateCell = row.createCell(3);
            dateCell.setCellStyle(dateStyle);
            dateCell.setCellValue(LocalDateTime.parse((String) data[3]));

            row.createCell(4).setCellValue((Double) data[4]); // amount
            row.createCell(5).setCellValue((String) data[5]); // ID
            row.createCell(6).setCellValue((String) data[6]); // comment
            row.createCell(7).setCellValue("My Trades");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    // BUY transactions
    @Test
    void parse_returnsStockPurchase() throws Exception {
        InputStream xlsx = buildXlsx(new Object[][] {{
                "Stock purchase", "AAPL.US", "Apple",
                "2024-03-18T14:46:46", -7043.75, "ID123",
                "OPEN BUY 43 @ 177.45"
        }});

        List<ParsedTransaction> result = parser.parse(xlsx);

        assertThat(result).hasSize(1);
        ParsedTransaction t = result.get(0);
        assertThat(t.getTransactionType()).isEqualTo("BUY");
        assertThat(t.getTicker()).isEqualTo("AAPL.US");
        assertThat(t.getInstrumentName()).isEqualTo("Apple");
        assertThat(t.getIsin()).isNull();
        assertThat(t.getQuantity()).isEqualByComparingTo(new BigDecimal("43"));
        assertThat(t.getPrice()).isEqualByComparingTo(new BigDecimal("177.45"));
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("7043.75"));
        assertThat(t.getCurrency()).isEqualTo("EUR");
        assertThat(t.getExternalId()).isEqualTo("ID123");
    }

    @Test
    void parse_returnsPartialFillBuy_usesFirstNumber() throws Exception {
        InputStream xlsx = buildXlsx(new Object[][] {{
                "Stock purchase", "FB2A.DE", "Meta",
                "2026-03-27T16:27:50", -129.92, "ID456",
                "OPEN BUY 0.284/14.3522 @ 457.45"
        }});

        List<ParsedTransaction> result = parser.parse(xlsx);

        assertThat(result).hasSize(1);
        // Should use first number 0.284, not total 14.3522
        assertThat(result.get(0).getQuantity())
                .isEqualByComparingTo(new BigDecimal("0.284"));
    }

    // SELL transactions
    @Test
    void parse_returnsStockSell() throws Exception {
        InputStream xlsx = buildXlsx(new Object[][] {{
                "Stock sell", "QDVE.DE", "S&P 500 IT Sector",
                "2026-03-27T16:27:18", 6692.31, "ID789",
                "CLOSE BUY 207 @ 32.330"
        }});

        List<ParsedTransaction> result = parser.parse(xlsx);

        assertThat(result).hasSize(1);
        ParsedTransaction t = result.get(0);
        assertThat(t.getTransactionType()).isEqualTo("SELL");
        assertThat(t.getQuantity()).isEqualByComparingTo(new BigDecimal("207"));
        assertThat(t.getPrice()).isEqualByComparingTo(new BigDecimal("32.330"));
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("6692.31"));
    }

    @Test
    void parse_returnsPartialFillSell_usesFirstNumber() throws Exception {
        InputStream xlsx = buildXlsx(new Object[][] {{
                "Stock sell", "AAPL.US", "Apple",
                "2024-04-15T13:32:22", 7000.50, "ID999",
                "CLOSE BUY 43/43.3639 @ 174.14"
        }});

        List<ParsedTransaction> result = parser.parse(xlsx);

        assertThat(result).hasSize(1);
        // Should use first number 43, not total 43.3639
        assertThat(result.get(0).getQuantity())
                .isEqualByComparingTo(new BigDecimal("43"));
    }

    // DIVIDEND transactions
    @Test
    void parse_returnsDividend() throws Exception {
        InputStream xlsx = buildXlsx(new Object[][] {{
                "Dividend", "FB2A.DE", "Meta",
                "2026-03-26T10:57:01", 6.51, "DIV001",
                "FB2A.DE USD 0.5250/ SHR"
        }});

        List<ParsedTransaction> result = parser.parse(xlsx);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionType()).isEqualTo("DIVIDEND");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("6.51"));
    }

    // Skipped rows
    @Test
    void parse_skipsDeposit() throws Exception {
        InputStream xlsx = buildXlsx(new Object[][] {{
                "Deposit", "", "", "2024-01-01T10:00:00", 1000.0, "DEP001", ""
        }});

        List<ParsedTransaction> result = parser.parse(xlsx);
        assertThat(result).isEmpty();
    }

    @Test
    void parse_skipsWithdrawal() throws Exception {
        InputStream xlsx = buildXlsx(new Object[][] {{
                "Withdrawal", "", "", "2024-01-01T10:00:00", -500.0, "WIT001", ""
        }});

        List<ParsedTransaction> result = parser.parse(xlsx);
        assertThat(result).isEmpty();
    }

    @Test
    void parse_skipsWithholdingTax() throws Exception {
        InputStream xlsx = buildXlsx(new Object[][] {{
                "Withholding tax", "FB2A.DE", "Meta",
                "2026-03-26T10:57:01", -0.98, "TAX001",
                "FB2A.DE USD WHT 15%"
        }});

        List<ParsedTransaction> result = parser.parse(xlsx);
        assertThat(result).isEmpty();
    }

    // Multiple rows mixed
    @Test
    void parse_returnsOnlyValidTransactions_fromMixedRows() throws Exception {
        InputStream xlsx = buildXlsx(new Object[][] {
                {"Stock purchase", "AAPL.US", "Apple",
                        "2024-03-18T14:46:46", -7043.75, "ID001",
                        "OPEN BUY 43 @ 177.45"},
                {"Deposit", "", "", "2024-01-01T10:00:00", 5000.0, "DEP001", ""},
                {"Stock sell", "AAPL.US", "Apple",
                        "2024-04-15T13:32:22", 7000.50, "ID002",
                        "CLOSE BUY 43 @ 174.14"},
                {"Withholding tax", "FB2A.DE", "Meta",
                        "2026-03-26T10:57:01", -0.98, "TAX001",
                        "FB2A.DE USD WHT 15%"}
        });

        List<ParsedTransaction> result = parser.parse(xlsx);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTransactionType()).isEqualTo("BUY");
        assertThat(result.get(1).getTransactionType()).isEqualTo("SELL");
    }

    // Empty file
    @Test
    void parse_returnsEmptyList_whenNoDataRows() throws Exception {
        InputStream xlsx = buildXlsx(new Object[0][]);
        List<ParsedTransaction> result = parser.parse(xlsx);
        assertThat(result).isEmpty();
    }
}