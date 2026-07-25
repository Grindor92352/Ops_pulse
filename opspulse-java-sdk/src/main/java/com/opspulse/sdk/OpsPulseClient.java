package com.opspulse.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Official Thread-Safe Java Client SDK for OpsPulse Incident Platform.
 */
public class OpsPulseClient {

    private static final Logger logger = LoggerFactory.getLogger(OpsPulseClient.class);
    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpsPulseClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:3000/api/ingest";
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<Boolean> captureException(Throwable throwable) {
        return captureException(throwable, "HIGH", new HashMap<>());
    }

    public CompletableFuture<Boolean> captureException(Throwable throwable, String severity, Map<String, String> tags) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("[OpsPulse SDK] API key is missing. Skipping telemetry capture.");
            return CompletableFuture.completedFuture(false);
        }

        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);

            Map<String, Object> payload = new HashMap<>();
            payload.put("message", throwable.getClass().getName() + ": " + throwable.getMessage());
            payload.put("stack", sw.toString());
            payload.put("severity", severity != null ? severity : "HIGH");
            payload.put("environment", System.getProperty("env", "PRODUCTION"));
            payload.put("tags", tags != null ? tags : new HashMap<>());

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            logger.info("[OpsPulse SDK] Telemetry report dispatched successfully. Status: {}", response.statusCode());
                            return true;
                        } else {
                            logger.error("[OpsPulse SDK] Failed to report telemetry. Status: {}, Body: {}", response.statusCode(), response.body());
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        logger.error("[OpsPulse SDK] Exception while dispatching telemetry", ex);
                        return false;
                    });
        } catch (Exception e) {
            logger.error("[OpsPulse SDK] Failed to serialize error payload", e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
