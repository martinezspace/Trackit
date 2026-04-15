package com.trackit.priceworker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvestmentServiceClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String internalKey;

    public InvestmentServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.internalKey = System.getenv("INTERNAL_SERVICE_KEY") != null
                ? System.getenv("INTERNAL_SERVICE_KEY")
                : "internal-service-key";
    }

    //GET /api/instruments/active
    //Returns list of InstrumentInfo - only id and ticker extracted from full response
    public List<InstrumentInfo> getActiveHoldingInstruments() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/instruments/active"))
                .header("X-Internal-Key", internalKey)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Failed to get instruments - HTTP " + response.statusCode());
        }

        List<InstrumentInfo> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(response.body());
        if (root.isArray()) {
            root.forEach(node -> {
                //InstrumentResponseDTO has fields: id, ticker
                String id = node.path("id").asText();
                String ticker = node.path("ticker").asText();
                if (!id.isEmpty() && !ticker.isEmpty()) {
                    result.add(new InstrumentInfo(id, ticker));
                }
            });
        }
        return result;
    }

    //POST /api/price-history
    //Sends instrumentId + closePrice + priceDate + source
    //currency is omitted - InvestmentService reads it from the instrument record
    public void savePriceHistory(String instrumentId, Double price) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("instrumentId", instrumentId);
        body.put("closePrice", price);
        body.put("priceDate", LocalDate.now().toString());
        body.put("source", "ALPHA_VANTAGE");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/price-history"))
                .header("Content-Type", "application/json")
                .header("X-Internal-Key", internalKey)
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException(
                    "Failed to save price for " + instrumentId +
                    " - HTTP" + response.statusCode());
        }
    }

    //Holds just what PriceWorker needs - id and ticker extracted from InstrumentResponseDTO
    public record InstrumentInfo(String instrumentId, String ticker) {}
}
