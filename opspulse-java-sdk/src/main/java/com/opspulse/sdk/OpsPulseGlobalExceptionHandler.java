package com.opspulse.sdk;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot Global Exception Handler for automated OpsPulse telemetry.
 */
@ControllerAdvice
public class OpsPulseGlobalExceptionHandler {

    private final OpsPulseClient opsPulseClient;

    public OpsPulseGlobalExceptionHandler(OpsPulseClient opsPulseClient) {
        this.opsPulseClient = opsPulseClient;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandledException(Exception ex) {
        // Automatically capture and send to OpsPulse platform
        Map<String, String> tags = new HashMap<>();
        tags.put("source", "Spring Boot ControllerAdvice");
        
        opsPulseClient.captureException(ex, "CRITICAL", tags);

        Map<String, Object> body = new HashMap<>();
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
