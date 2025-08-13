package com.example.signalling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix = "signalling")
@ConfigurationPropertiesScan
public record SignallingProperties(
    Long staleTimeoutMs,
    Long cleanupIntervalMs,
    RateLimit rateLimit
) {
    public record RateLimit(
        Integer windowSeconds,
        Integer maxMessages
    ) {}
}