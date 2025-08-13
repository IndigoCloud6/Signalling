package com.example.signalling.service;

import com.example.signalling.config.SignallingProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {
    
    private final SignallingProperties.RateLimit rateLimitConfig;
    private final ConcurrentHashMap<String, SlidingWindowCounter> sessionCounters = new ConcurrentHashMap<>();
    private final Counter rateLimitExceededCounter;

    public RateLimitService(SignallingProperties properties, MeterRegistry meterRegistry) {
        this.rateLimitConfig = properties.rateLimit();
        this.rateLimitExceededCounter = Counter.builder("signalling.rate_limit.exceeded")
                .description("Number of rate limit violations")
                .register(meterRegistry);
    }

    public boolean isAllowed(String sessionId) {
        SlidingWindowCounter counter = sessionCounters.computeIfAbsent(sessionId, 
            k -> new SlidingWindowCounter(rateLimitConfig.windowSeconds(), rateLimitConfig.maxMessages()));
        
        boolean allowed = counter.tryConsume();
        if (!allowed) {
            rateLimitExceededCounter.increment();
        }
        return allowed;
    }

    public void removeSession(String sessionId) {
        sessionCounters.remove(sessionId);
    }

    private static class SlidingWindowCounter {
        private final int windowSeconds;
        private final int maxMessages;
        private final ConcurrentHashMap<Long, AtomicInteger> buckets = new ConcurrentHashMap<>();

        public SlidingWindowCounter(int windowSeconds, int maxMessages) {
            this.windowSeconds = windowSeconds;
            this.maxMessages = maxMessages;
        }

        public boolean tryConsume() {
            long currentSecond = Instant.now().getEpochSecond();
            
            // Remove old buckets outside the window
            buckets.entrySet().removeIf(entry -> entry.getKey() < currentSecond - windowSeconds);
            
            // Count current messages in the window
            int currentCount = buckets.values().stream()
                    .mapToInt(AtomicInteger::get)
                    .sum();
            
            if (currentCount >= maxMessages) {
                return false; // Rate limit exceeded
            }
            
            // Increment counter for current second
            buckets.computeIfAbsent(currentSecond, k -> new AtomicInteger(0)).incrementAndGet();
            return true;
        }
    }
}