package com.trackit.investmentservice.csv;

import java.io.InputStream;
import java.util.List;

public interface CsvParser {
    List<ParsedTransaction> parse(InputStream csvStream);
}
