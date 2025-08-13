package com.example.signalling.model;

import org.springframework.web.reactive.socket.WebSocketSession;

import java.time.Instant;

public class StreamerSession {
    private final String streamerId;
    private final WebSocketSession webSocketSession;
    private final Instant connectedAt;
    private volatile Instant lastActiveAt;

    public StreamerSession(String streamerId, WebSocketSession webSocketSession) {
        this.streamerId = streamerId;
        this.webSocketSession = webSocketSession;
        this.connectedAt = Instant.now();
        this.lastActiveAt = this.connectedAt;
    }

    public String getStreamerId() {
        return streamerId;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void updateLastActive() {
        this.lastActiveAt = Instant.now();
    }

    public boolean isStale(long staleTimeoutMs) {
        return Instant.now().toEpochMilli() - lastActiveAt.toEpochMilli() > staleTimeoutMs;
    }
}