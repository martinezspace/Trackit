package com.trackit.priceworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AlphaVantageClient {

    private static final String BASE_URL = "https://www.alphavantage.co/query";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AlphaVantageClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    //Returns latest price for ticker, null on any error
    //Null means caller skips this ticker and logs a warning
    public Double getLatestPrice(String ticker) {
        try {
            String symbol = normaliseSymbol(ticker);
            String url = BASE_URL +
                    "?function=GLOBAL_QUOTE" +
                    "&symbol=" + symbol +
                    "&apikey=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return null;

            JsonNode root = objectMapper.readTree(response.body());

            //Rate limited or daily quota exceeded
            if (root.has("Information") || root.has("Note")) return null;

            JsonNode quote = root.path("Global Quote");
            if (quote.isMissingNode() || quote.isEmpty()) return null;

            String priceStr = quote.path("05. price").asText();
            if (priceStr.isEmpty() || priceStr.equals("null")) return null;

            return Double.parseDouble(priceStr);

        } catch (Exception e) {
            return null;
        }
    }


    //Normalises XTB ticker formats to Alpha Vantage format
    private String normaliseSymbol(String ticker) {
        if (ticker == null || ticker.isEmpty()) return "";
        if (ticker.endsWith(".US")) return ticker.replace(".US", "");
        if (ticker.endsWith(".DE")) return ticker.replace(".DE", "DEX");
        if (ticker.endsWith(".FR")) return ticker.replace(".FR", "PAR");
        if (ticker.endsWith(".NL")) return ticker.replace(".NL", "AMS");
        if (ticker.endsWith(".DK")) return ticker.replace(".DK", "CPH");
        if (ticker.endsWith(".ES")) return ticker.replace(".ES", "BME");
        return ticker;
    }
}
