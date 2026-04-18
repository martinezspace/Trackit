package com.trackit.priceworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

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

    // Returns latest price for ticker, null on any error
    public Double getLatestPrice(String ticker) {
        try {
            String symbol = normaliseSymbol(ticker);

            if (symbol.isEmpty()) {
                System.out.println("Invalid symbol after normalization: " + ticker);
                return null;
            }

            String url = BASE_URL +
                    "?function=GLOBAL_QUOTE" +
                    "&symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) +
                    "&apikey=" + apiKey;

            System.out.println("Fetching price for: " + ticker + " -> " + symbol);
            System.out.println("Request URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Non-200 response: " + response.statusCode());
                return null;
            }

            String body = response.body();
            System.out.println("Response body: " + body);

            JsonNode root = objectMapper.readTree(body);

            // Rate limit / API messages
            if (root.has("Information") || root.has("Note")) {
                System.out.println("Alpha Vantage limit hit or info message returned.");
                return null;
            }

            JsonNode quote = root.path("Global Quote");
            if (quote.isMissingNode() || quote.isEmpty()) {
                System.out.println("Empty Global Quote for: " + symbol);
                return null;
            }

            String priceStr = quote.path("05. price").asText();
            if (priceStr == null || priceStr.isEmpty() || priceStr.equalsIgnoreCase("null")) {
                System.out.println("Price missing in response for: " + symbol);
                return null;
            }

            return Double.parseDouble(priceStr);

        } catch (Exception e) {
            System.out.println("Error fetching price for: " + ticker);
            e.printStackTrace();
            return null;
        }
    }

    // Normalises XTB ticker formats to something Alpha Vantage *might* understand
    private String normaliseSymbol(String ticker) {
        if (ticker == null || ticker.isEmpty()) return "";

        ticker = ticker.trim().toUpperCase();

        if (ticker.endsWith(".US")) return ticker.replace(".US", "");
        if (ticker.endsWith(".DE")) return ticker;
        if (ticker.endsWith(".FR")) return ticker.replace(".FR", ".PA"); // Paris
        if (ticker.endsWith(".NL")) return ticker.replace(".NL", ".AS"); // Amsterdam
        if (ticker.endsWith(".DK")) return ticker.replace(".DK", ".CO"); // Copenhagen
        if (ticker.endsWith(".ES")) return ticker.replace(".ES", ".MC"); // Madrid

        return ticker;
    }
}