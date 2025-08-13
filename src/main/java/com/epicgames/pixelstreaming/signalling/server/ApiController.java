// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.server;

import com.epicgames.pixelstreaming.signalling.config.SignallingConfig;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.service.ConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for signalling server management.
 * Provides endpoints for health checks, statistics, connections, and configuration.
 */
@RestController
@RequestMapping("${signalling.api.base-path:/api}")
public class ApiController {

    private final ConnectionManager connectionManager;
    private final SignallingConfig config;
    private final MessageHelper messageHelper;

    public ApiController(ConnectionManager connectionManager, SignallingConfig config, MessageHelper messageHelper) {
        this.connectionManager = connectionManager;
        this.config = config;
        this.messageHelper = messageHelper;
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        if (!config.getApi().isEnableHealthEndpoint()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "pixelstreaming-signalling");
        health.put("version", "0.1.2");

        return ResponseEntity.ok(health);
    }

    /**
     * Server statistics endpoint.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        if (!config.getApi().isEnableStatsEndpoint()) {
            return ResponseEntity.notFound().build();
        }

        ConnectionManager.ConnectionStats stats = connectionManager.getConnectionStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("connections", Map.of(
            "players", stats.getPlayerConnections(),
            "streamers", stats.getStreamerConnections(),
            "sfu", stats.getSfuConnections(),
            "total", stats.getPlayerConnections() + stats.getStreamerConnections() + stats.getSfuConnections()
        ));
        response.put("subscriptions", stats.getTotalSubscriptions());
        response.put("maxSubscribersPerStreamer", config.getServer().getMaxSubscribers());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Active connections endpoint.
     */
    @GetMapping("/connections")
    public ResponseEntity<Map<String, Object>> connections() {
        if (!config.getApi().isEnableConnectionsEndpoint()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("players", connectionManager.getPlayerConnectionCount());
        response.put("streamers", connectionManager.getStreamerConnectionCount());
        response.put("sfu", connectionManager.getSfuConnectionCount());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Configuration endpoint.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        if (!config.getApi().isEnableConfigEndpoint()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("server", Map.of(
            "host", config.getServer().getHost(),
            "streamerPort", config.getServer().getStreamerPort(),
            "playerPort", config.getServer().getPlayerPort(),
            "sfuPort", config.getServer().getSfuPort(),
            "httpPort", config.getServer().getHttpPort(),
            "maxSubscribers", config.getServer().getMaxSubscribers(),
            "enableSfu", config.getServer().isEnableSfu()
        ));
        response.put("websocket", Map.of(
            "maxFrameSize", config.getWebsocket().getMaxFrameSize(),
            "pingIntervalSeconds", config.getWebsocket().getPingIntervalSeconds(),
            "connectionTimeoutSeconds", config.getWebsocket().getConnectionTimeoutSeconds()
        ));
        response.put("security", Map.of(
            "enableAuth", config.getSecurity().isEnableAuth(),
            "rateLimitPerMinute", config.getSecurity().getRateLimitPerMinute(),
            "enableCors", config.getSecurity().isEnableCors()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Update configuration endpoint (if enabled).
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, Object> configUpdate) {
        if (!config.getApi().isEnableConfigEndpoint()) {
            return ResponseEntity.notFound().build();
        }

        // For now, return a message indicating that dynamic config update is not implemented
        Map<String, String> response = new HashMap<>();
        response.put("message", "Dynamic configuration update not implemented yet");
        response.put("status", "not_implemented");

        return ResponseEntity.ok(response);
    }

    /**
     * Ping endpoint for connectivity testing.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "pong");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}