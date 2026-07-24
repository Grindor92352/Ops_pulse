package com.opspulse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestTelemetryDto {
    private String apiKey;
    private String message;
    private String name;
    private String stack;
    private String url;
    private String userAgent;
    private List<Map<String, Object>> breadcrumbs;
    private Map<String, Object> environment;
}
