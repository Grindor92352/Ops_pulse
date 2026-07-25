package com.opspulse.sdk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class OpsPulseClientTest {

    @Test
    public void testCaptureException() {
        System.out.println("☕ Testing OpsPulse Java SDK Exception Dispatch...");

        OpsPulseClient client = new OpsPulseClient("opspulse_sk_demo_key_12345", "http://localhost:3000/api/ingest");

        Map<String, String> tags = new HashMap<>();
        tags.put("language", "Java 21");
        tags.put("framework", "Spring Boot 3");
        tags.put("test_type", "Integration Test");

        Throwable testException = new NullPointerException("NullPointerException in Java OrderService.processPayment(OrderService.java:88)");

        Boolean result = client.captureException(testException, "CRITICAL", tags).join();

        System.out.println("✅ OpsPulse Java SDK Dispatch Result: " + result);
        Assertions.assertTrue(result, "Java SDK captureException should return true on successful HTTP response");
    }
}
