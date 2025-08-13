package com.example.signalling.registry;

import com.example.signalling.model.StreamerSession;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StreamerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(StreamerRegistry.class);
    
    private final ConcurrentHashMap<String, StreamerSession> streamers = new ConcurrentHashMap<>();

    public StreamerRegistry(MeterRegistry meterRegistry) {
        Gauge.builder("signalling.streamers.current", this, StreamerRegistry::getStreamerCount)
                .description("Current number of connected streamers")
                .register(meterRegistry);
    }

    public void addStreamer(StreamerSession session) {
        streamers.put(session.getStreamerId(), session);
        logger.info("Streamer registered: {}", session.getStreamerId());
    }

    public void removeStreamer(String streamerId) {
        StreamerSession removed = streamers.remove(streamerId);
        if (removed != null) {
            logger.info("Streamer removed: {}", streamerId);
        }
    }

    public Optional<StreamerSession> getStreamer(String streamerId) {
        return Optional.ofNullable(streamers.get(streamerId));
    }

    public Collection<StreamerSession> getAllStreamers() {
        return streamers.values();
    }

    public boolean hasStreamer(String streamerId) {
        return streamers.containsKey(streamerId);
    }

    public double getStreamerCount() {
        return streamers.size();
    }

    public void removeStaleStreamers(long staleTimeoutMs) {
        streamers.entrySet().removeIf(entry -> {
            boolean isStale = entry.getValue().isStale(staleTimeoutMs);
            if (isStale) {
                logger.info("Removing stale streamer: {}", entry.getKey());
            }
            return isStale;
        });
    }
}