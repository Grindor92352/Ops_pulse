package com.opspulse.sdk;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpsPulseProperties.class)
@ConditionalOnProperty(prefix = "opspulse.sdk", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpsPulseAutoConfiguration {

    @Bean
    public OpsPulseClient opsPulseClient(OpsPulseProperties properties) {
        return new OpsPulseClient(properties.getApiKey(), properties.getBaseUrl());
    }

    @Bean
    public OpsPulseGlobalExceptionHandler opsPulseGlobalExceptionHandler(OpsPulseClient client) {
        return new OpsPulseGlobalExceptionHandler(client);
    }
}
