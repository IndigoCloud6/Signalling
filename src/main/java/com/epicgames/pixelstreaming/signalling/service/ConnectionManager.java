// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.service;

import com.epicgames.pixelstreaming.signalling.config.SignallingConfig;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for managing all WebSocket connections.
 * Handles player, streamer, and SFU connection lifecycle and routing.
 */
@Service
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final Map<String, PlayerConnection> playerConnections = new ConcurrentHashMap<>();
    private final Map<String, StreamerConnection> streamerConnections = new ConcurrentHashMap<>();
    private final Map<String, SFUConnection> sfuConnections = new ConcurrentHashMap<>();

    private final SignallingConfig config;
    private final MessageHelper messageHelper;
    private final MeterRegistry meterRegistry;
    private final ScheduledExecutorService scheduler;

    // Metrics
    private final Counter playerConnectionsTotal;
    private final Counter streamerConnectionsTotal;
    private final Counter sfuConnectionsTotal;
    private final Counter disconnectionsTotal;

    public ConnectionManager(SignallingConfig config, MessageHelper messageHelper, MeterRegistry meterRegistry) {
        this.config = config;
        this.messageHelper = messageHelper;
        this.meterRegistry = meterRegistry;
        this.scheduler = Executors.newScheduledThreadPool(2);

        // Initialize metrics
        this.playerConnectionsTotal = Counter.builder("signalling.connections.player.total")
            .description("Total number of player connections")
            .register(meterRegistry);

        this.streamerConnectionsTotal = Counter.builder("signalling.connections.streamer.total")
            .description("Total number of streamer connections")
            .register(meterRegistry);

        this.sfuConnectionsTotal = Counter.builder("signalling.connections.sfu.total")
            .description("Total number of SFU connections")
            .register(meterRegistry);

        this.disconnectionsTotal = Counter.builder("signalling.disconnections.total")
            .description("Total number of disconnections")
            .register(meterRegistry);

        // Register gauges for current connection counts
        Gauge.builder("signalling.connections.player.current", this, ConnectionManager::getPlayerConnectionCount)
            .description("Current number of player connections")
            .register(meterRegistry);

        Gauge.builder("signalling.connections.streamer.current", this, ConnectionManager::getStreamerConnectionCount)
            .description("Current number of streamer connections")
            .register(meterRegistry);

        Gauge.builder("signalling.connections.sfu.current", this, ConnectionManager::getSfuConnectionCount)
            .description("Current number of SFU connections")
            .register(meterRegistry);

        // Start connection cleanup task
        startConnectionCleanupTask();
    }

    /**
     * Add a player connection.
     */
    public void addPlayerConnection(PlayerConnection connection) {
        playerConnections.put(connection.getId(), connection);
        playerConnectionsTotal.increment();
        logger.info("Added player connection {} (total: {})", 
                   connection.getId(), playerConnections.size());
    }

    /**
     * Remove a player connection.
     */
    public void removePlayerConnection(PlayerConnection connection) {
        PlayerConnection removed = playerConnections.remove(connection.getId());
        if (removed != null) {
            disconnectionsTotal.increment();
            logger.info("Removed player connection {} (total: {})", 
                       connection.getId(), playerConnections.size());
        }
    }

    /**
     * Add a streamer connection.
     */
    public void addStreamerConnection(StreamerConnection connection) {
        streamerConnections.put(connection.getId(), connection);
        streamerConnectionsTotal.increment();
        logger.info("Added streamer connection {} (total: {})", 
                   connection.getId(), streamerConnections.size());
    }

    /**
     * Remove a streamer connection.
     */
    public void removeStreamerConnection(StreamerConnection connection) {
        StreamerConnection removed = streamerConnections.remove(connection.getId());
        if (removed != null) {
            disconnectionsTotal.increment();
            logger.info("Removed streamer connection {} (total: {})", 
                       connection.getId(), streamerConnections.size());
        }
    }

    /**
     * Add an SFU connection.
     */
    public void addSfuConnection(SFUConnection connection) {
        sfuConnections.put(connection.getId(), connection);
        sfuConnectionsTotal.increment();
        logger.info("Added SFU connection {} (total: {})", 
                   connection.getId(), sfuConnections.size());
    }

    /**
     * Remove an SFU connection.
     */
    public void removeSfuConnection(SFUConnection connection) {
        SFUConnection removed = sfuConnections.remove(connection.getId());
        if (removed != null) {
            disconnectionsTotal.increment();
            logger.info("Removed SFU connection {} (total: {})", 
                       connection.getId(), sfuConnections.size());
        }
    }

    /**
     * Get an available streamer for player subscription.
     */
    public StreamerConnection getAvailableStreamer() {
        return streamerConnections.values().stream()
            .filter(StreamerConnection::canAcceptSubscribers)
            .findFirst()
            .orElse(null);
    }

    /**
     * Get a specific streamer by ID.
     */
    public StreamerConnection getStreamerById(String streamerId) {
        return streamerConnections.get(streamerId);
    }

    /**
     * Get a specific player by ID.
     */
    public PlayerConnection getPlayerById(String playerId) {
        return playerConnections.get(playerId);
    }

    /**
     * Get a specific SFU by ID.
     */
    public SFUConnection getSfuById(String sfuId) {
        return sfuConnections.get(sfuId);
    }

    /**
     * Notify about player count changes.
     */
    public void notifyPlayerCountChanged() {
        // Update all streamers with current player counts
        streamerConnections.values().forEach(streamer -> {
            if (streamer.isActive()) {
                // Player count message is sent by the streamer itself
                logger.debug("Player count changed, streamer {} has {} subscribers", 
                           streamer.getId(), streamer.getSubscriberCount());
            }
        });
    }

    /**
     * Get connection statistics.
     */
    public ConnectionStats getConnectionStats() {
        return new ConnectionStats(
            getPlayerConnectionCount(),
            getStreamerConnectionCount(),
            getSfuConnectionCount(),
            getTotalSubscriptions()
        );
    }

    /**
     * Get current player connection count.
     */
    public int getPlayerConnectionCount() {
        return playerConnections.size();
    }

    /**
     * Get current streamer connection count.
     */
    public int getStreamerConnectionCount() {
        return streamerConnections.size();
    }

    /**
     * Get current SFU connection count.
     */
    public int getSfuConnectionCount() {
        return sfuConnections.size();
    }

    /**
     * Get total number of player subscriptions across all streamers.
     */
    public int getTotalSubscriptions() {
        return streamerConnections.values().stream()
            .mapToInt(StreamerConnection::getSubscriberCount)
            .sum();
    }

    /**
     * Start periodic connection cleanup task.
     */
    private void startConnectionCleanupTask() {
        scheduler.scheduleAtFixedRate(this::cleanupInactiveConnections, 30, 30, TimeUnit.SECONDS);
        logger.info("Started connection cleanup task");
    }

    /**
     * Clean up inactive connections.
     */
    private void cleanupInactiveConnections() {
        long now = System.currentTimeMillis();
        long timeoutMillis = config.getWebsocket().getConnectionTimeoutSeconds() * 1000L;

        // Clean up inactive player connections
        playerConnections.values().removeIf(connection -> {
            if (!connection.isActive() || (now - connection.getLastActivity() > timeoutMillis)) {
                logger.debug("Cleaning up inactive player connection {}", connection.getId());
                connection.close();
                return true;
            }
            return false;
        });

        // Clean up inactive streamer connections
        streamerConnections.values().removeIf(connection -> {
            if (!connection.isActive() || (now - connection.getLastActivity() > timeoutMillis)) {
                logger.debug("Cleaning up inactive streamer connection {}", connection.getId());
                connection.close();
                return true;
            }
            return false;
        });

        // Clean up inactive SFU connections
        sfuConnections.values().removeIf(connection -> {
            if (!connection.isActive() || (now - connection.getLastActivity() > timeoutMillis)) {
                logger.debug("Cleaning up inactive SFU connection {}", connection.getId());
                connection.close();
                return true;
            }
            return false;
        });
    }

    /**
     * Shutdown the connection manager.
     */
    public void shutdown() {
        logger.info("Shutting down connection manager");
        
        // Close all connections
        playerConnections.values().forEach(IConnection::close);
        streamerConnections.values().forEach(IConnection::close);
        sfuConnections.values().forEach(IConnection::close);
        
        // Clear collections
        playerConnections.clear();
        streamerConnections.clear();
        sfuConnections.clear();
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Connection statistics data class.
     */
    public static class ConnectionStats {
        private final int playerConnections;
        private final int streamerConnections;
        private final int sfuConnections;
        private final int totalSubscriptions;

        public ConnectionStats(int playerConnections, int streamerConnections, 
                             int sfuConnections, int totalSubscriptions) {
            this.playerConnections = playerConnections;
            this.streamerConnections = streamerConnections;
            this.sfuConnections = sfuConnections;
            this.totalSubscriptions = totalSubscriptions;
        }

        // Getters
        public int getPlayerConnections() { return playerConnections; }
        public int getStreamerConnections() { return streamerConnections; }
        public int getSfuConnections() { return sfuConnections; }
        public int getTotalSubscriptions() { return totalSubscriptions; }
    }
}