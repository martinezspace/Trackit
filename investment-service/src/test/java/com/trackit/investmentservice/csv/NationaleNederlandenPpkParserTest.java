package com.trackit.investmentservice.csv;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NationaleNederlandenPpkParserTest {

    private NationaleNederlandenPpkParser parser;

    @BeforeEach
    void setUp() {
        parser = new NationaleNederlandenPpkParser();
    }

    // Helper — builds an in-memory .xls file matching NN PPK structure
    private InputStream buildXls(String[][] dataRows) throws Exception {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Arkusz1");

        // Row 0: Pracodawca metadata
        Row r0 = sheet.createRow(0);
        r0.createCell(0).setCellValue("");
        r0.createCell(1).setCellValue("Pracodawca");
        r0.createCell(2).setCellValue("Test Company");

        // Row 1: Account number metadata
        Row r1 = sheet.createRow(1);
        r1.createCell(1).setCellValue("Nr rachunku PPK");

        // Rows 2-3: empty
        sheet.createRow(2);
        sheet.createRow(3);

        // Row 4: headers
        Row header = sheet.createRow(4);
        header.createCell(0).setCellValue("Data zlecenia");
        header.createCell(1).setCellValue("Data wyceny");
        header.createCell(2).setCellValue("Typ transakcji");
        header.createCell(3).setCellValue("Okres składki");
        header.createCell(4).setCellValue("Kwota (PLN)");
        header.createCell(5).setCellValue("Wartość rachunku po transakcji (PLN)");
        header.createCell(6).setCellValue("Nazwa funduszu");
        header.createCell(7).setCellValue("Cena jednostki (PLN)");
        header.createCell(8).setCellValue("Liczba jednostek");
        header.createCell(9).setCellValue("Wartość jednostek (PLN)");
        header.createCell(10).setCellValue("Podatek (PLN)");

        // Data rows starting at row 5
        for (int i = 0; i < dataRows.length; i++) {
            Row row = sheet.createRow(5 + i);
            String[] data = dataRows[i];
            row.createCell(0).setCellValue(data[0]); // date
            row.createCell(1).setCellValue(data[1]); // valuation date
            row.createCell(2).setCellValue(data[2]); // transaction type
            row.createCell(3).setCellValue(data[3]); // period
            row.createCell(4).setCellValue(Double.parseDouble(data[4])); // amount
            row.createCell(5).setCellValue(0.0); // account value
            row.createCell(6).setCellValue(data[6]); // fund name
            row.createCell(7).setCellValue(Double.parseDouble(data[7])); // unit price
            row.createCell(8).setCellValue(Double.parseDouble(data[8])); // units
            row.createCell(9).setCellValue(0.0); // unit value
            row.createCell(10).setCellValue(0.0); // tax
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    private static final String FUND_NAME =
            "Nationale-Nederlanden Dobrowolny Fundusz Emerytalny Nasze Jutro 2065";

    // BUY transactions
    @Test
    void parse_returnsEmployeeContribution() throws Exception {
        InputStream xls = buildXls(new String[][] {{
                "2026-03-27", "2026-03-31",
                "Wpłata podstawowa pracownika", "03-2026",
                "188.24", "", FUND_NAME, "16.34", "11.5202"
        }});

        List<ParsedTransaction> result = parser.parse(xls);

        assertThat(result).hasSize(1);
        ParsedTransaction t = result.get(0);
        assertThat(t.getTransactionType()).isEqualTo("BUY");
        assertThat(t.getInstrumentName()).isEqualTo(FUND_NAME);
        assertThat(t.getIsin()).isNull();
        assertThat(t.getTicker()).isNull();
        assertThat(t.getCurrency()).isEqualTo("PLN");
        assertThat(t.getAmount()).isEqualByComparingTo(new BigDecimal("188.24"));
        assertThat(t.getPrice()).isEqualByComparingTo(new BigDecimal("16.34"));
        assertThat(t.getQuantity()).isEqualByComparingTo(new BigDecimal("11.5202"));
    }

    @Test
    void parse_returnsEmployerContribution() throws Exception {
        InputStream xls = buildXls(new String[][] {{
                "2026-03-27", "2026-03-31",
                "Wpłata podstawowa pracodawcy", "03-2026",
                "141.18", "", FUND_NAME, "16.34", "8.6401"
        }});

        List<ParsedTransaction> result = parser.parse(xls);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionType()).isEqualTo("BUY");
    }

    @Test
    void parse_returnsStateBonus() throws Exception {
        InputStream xls = buildXls(new String[][] {{
                "2026-03-27", "2026-03-30",
                "Dopłata roczna od państwa", "",
                "240.00", "", FUND_NAME, "16.07", "14.9347"
        }});

        List<ParsedTransaction> result = parser.parse(xls);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionType()).isEqualTo("BUY");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("240.00"));
    }

    // External ID deduplication
    @Test
    void parse_generatesDifferentExternalIds_forDifferentTransactions() throws Exception {
        InputStream xls = buildXls(new String[][] {
                {"2026-03-27", "2026-03-31", "Wpłata podstawowa pracownika", "03-2026",
                        "188.24", "", FUND_NAME, "16.34", "11.5202"},
                {"2026-03-27", "2026-03-31", "Wpłata podstawowa pracodawcy", "03-2026",
                        "141.18", "", FUND_NAME, "16.34", "8.6401"}
        });

        List<ParsedTransaction> result = parser.parse(xls);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getExternalId())
                .isNotEqualTo(result.get(1).getExternalId());
    }

    @Test
    void parse_generatesSameExternalId_forSameTransaction() throws Exception {
        String[][] rows = {{
                "2026-03-27", "2026-03-31",
                "Wpłata podstawowa pracownika", "03-2026",
                "188.24", "", FUND_NAME, "16.34", "11.5202"
        }};

        List<ParsedTransaction> result1 = parser.parse(buildXls(rows));
        List<ParsedTransaction> result2 = parser.parse(buildXls(rows));

        assertThat(result1.get(0).getExternalId())
                .isEqualTo(result2.get(0).getExternalId());
    }

    // Empty / invalid rows skipped
    @Test
    void parse_returnsEmptyList_whenNoDataRows() throws Exception {
        InputStream xls = buildXls(new String[0][]);
        List<ParsedTransaction> result = parser.parse(xls);
        assertThat(result).isEmpty();
    }

    // Multiple rows
    @Test
    void parse_returnsAllValidTransactions() throws Exception {
        InputStream xls = buildXls(new String[][] {
                {"2026-03-27", "2026-03-31", "Wpłata podstawowa pracownika", "03-2026",
                        "188.24", "", FUND_NAME, "16.34", "11.5202"},
                {"2026-03-27", "2026-03-31", "Wpłata podstawowa pracodawcy", "03-2026",
                        "141.18", "", FUND_NAME, "16.34", "8.6401"},
                {"2026-03-27", "2026-03-30", "Dopłata roczna od państwa", "",
                        "240.00", "", FUND_NAME, "16.07", "14.9347"}
        });

        List<ParsedTransaction> result = parser.parse(xls);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(t -> t.getTransactionType().equals("BUY"));
        assertThat(result).allMatch(t -> t.getIsin() == null);
        assertThat(result).allMatch(t -> t.getCurrency().equals("PLN"));
    }
}