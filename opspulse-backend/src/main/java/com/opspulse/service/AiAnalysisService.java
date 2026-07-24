package com.opspulse.service;

import com.opspulse.dto.AiAnalysisResultDto;
import com.opspulse.entity.EnvironmentType;
import com.opspulse.entity.IssuePriority;
import com.opspulse.entity.IssueSeverity;
import com.opspulse.entity.IssueSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AiAnalysisService.class);

    @Value("${opspulse.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${opspulse.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public AiAnalysisResultDto analyzeIncident(Map<String, Object> rawData, IssueSource source, List<String> techStack) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            logger.warn("GEMINI_API_KEY is missing. Generating intelligent local heuristic fallback analysis.");
            return generateFallbackAnalysis(rawData, source);
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey;

            String promptText = buildPrompt(rawData, source, techStack);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contentPart = Map.of("text", promptText);
            Map<String, Object> content = Map.of("parts", List.of(contentPart));
            requestBody.put("contents", List.of(content));

            Map<String, Object> generationConfig = Map.of("responseMimeType", "application/json");
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String rawResponseBody = restTemplate.postForObject(url, entity, String.class);
            return parseGeminiResponse(rawResponseBody);

        } catch (Exception ex) {
            logger.error("Error invoking Gemini AI API: {}", ex.getMessage(), ex);
            return generateFallbackAnalysis(rawData, source);
        }
    }

    private String buildPrompt(Map<String, Object> rawData, IssueSource source, List<String> techStack) {
        String techStackStr = (techStack != null && !techStack.isEmpty()) ? String.join(", ", techStack) : "Java, React, Node.js";
        String rawJson = "";
        try {
            rawJson = objectMapper.writeValueAsString(rawData);
        } catch (Exception e) {
            rawJson = rawData.toString();
        }

        return """
                You are an expert site reliability engineer and software architect.
                Analyze the following incident telemetry data from source: %s.
                Project tech stack: %s.
                
                Incident Raw Data:
                %s
                
                Your task is to:
                1. Provide a concise, professional title for the issue.
                2. Provide a detailed description of what happened.
                3. Categorize severity as one of: LOW, MEDIUM, HIGH, CRITICAL.
                4. Categorize priority as one of: LOW, MEDIUM, HIGH, URGENT.
                5. Identify Environment as one of: PRODUCTION, STAGING, DEVELOPMENT.
                6. Identify likely root cause from stack trace and logs.
                7. Suggest potential fixes or resolution steps.
                
                Respond ONLY with a valid JSON object matching this schema:
                {
                  "title": "string",
                  "description": "string",
                  "severity": "CRITICAL" | "HIGH" | "MEDIUM" | "LOW",
                  "priority": "LOW" | "MEDIUM" | "HIGH" | "URGENT",
                  "environment": "PRODUCTION" | "STAGING" | "DEVELOPMENT",
                  "rootCause": "string",
                  "suggestedFixes": "string"
                }
                """.formatted(source, techStackStr, rawJson);
    }

    private AiAnalysisResultDto parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String jsonText = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            JsonNode parsed = objectMapper.readTree(jsonText);

            return AiAnalysisResultDto.builder()
                    .title(parsed.path("title").asText("Telemetry Incident"))
                    .description(parsed.path("description").asText("Automated incident detected."))
                    .severity(parseEnum(parsed.path("severity").asText("MEDIUM"), IssueSeverity.MEDIUM))
                    .priority(parseEnum(parsed.path("priority").asText("MEDIUM"), IssuePriority.MEDIUM))
                    .environment(parseEnum(parsed.path("environment").asText("PRODUCTION"), EnvironmentType.PRODUCTION))
                    .rootCause(parsed.path("rootCause").asText("Stack trace points to unhandled runtime exception."))
                    .suggestedFixes(parsed.path("suggestedFixes").asText("Add try-catch block and validate input parameters."))
                    .build();
        } catch (Exception ex) {
            logger.error("Failed to parse Gemini response: {}", ex.getMessage());
            return generateFallbackAnalysis(Map.of(), IssueSource.SDK);
        }
    }

    private <T extends Enum<T>> T parseEnum(String value, T defaultValue) {
        try {
            return Enum.valueOf(defaultValue.getDeclaringClass(), value.toUpperCase());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private AiAnalysisResultDto generateFallbackAnalysis(Map<String, Object> rawData, IssueSource source) {
        String msg = rawData.containsKey("message") ? String.valueOf(rawData.get("message")) : "Unhandled Runtime Error";
        String stack = rawData.containsKey("stack") ? String.valueOf(rawData.get("stack")) : "No stack trace provided";

        IssueSeverity severity = stack.contains("NullPointer") || stack.contains("TypeError") ? IssueSeverity.HIGH : IssueSeverity.MEDIUM;

        return AiAnalysisResultDto.builder()
                .title("Incident: " + msg)
                .description("Automatic incident generated from " + source + " telemetry stream.")
                .severity(severity)
                .priority(severity == IssueSeverity.HIGH ? IssuePriority.HIGH : IssuePriority.MEDIUM)
                .environment(EnvironmentType.PRODUCTION)
                .rootCause("Automated Stack Analysis: " + (stack.length() > 200 ? stack.substring(0, 200) + "..." : stack))
                .suggestedFixes("Inspect component lifecycle and verify non-null references before property invocation.")
                .build();
    }
}
