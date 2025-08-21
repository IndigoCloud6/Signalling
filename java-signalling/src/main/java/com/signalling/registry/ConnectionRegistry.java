package com.signalling.registry;

import com.signalling.service.connection.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing active connections.
 * Provides thread-safe access to player, streamer, and SFU connections.
 */
@Component
public class ConnectionRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionRegistry.class);
    
    private final Map<String, ConnectionHandler> players = new ConcurrentHashMap<>();
    private final Map<String, ConnectionHandler> streamers = new ConcurrentHashMap<>();
    private final Map<String, ConnectionHandler> sfus = new ConcurrentHashMap<>();
    
    /**
     * Registers a new connection handler.
     */
    public void register(ConnectionHandler handler) {
        String connectionId = handler.getConnectionId();
        
        switch (handler.getConnectionType()) {
            case PLAYER:
                players.put(connectionId, handler);
                logger.info("Registered player connection: {}", connectionId);
                break;
            case STREAMER:
                streamers.put(connectionId, handler);
                logger.info("Registered streamer connection: {}", connectionId);
                break;
            case SFU:
                // SFU acts as both player and streamer
                sfus.put(connectionId, handler);
                players.put(connectionId, handler);
                streamers.put(connectionId, handler);
                logger.info("Registered SFU connection: {}", connectionId);
                break;
        }
    }
    
    /**
     * Unregisters a connection handler.
     */
    public void unregister(ConnectionHandler handler) {
        String connectionId = handler.getConnectionId();
        
        switch (handler.getConnectionType()) {
            case PLAYER:
                players.remove(connectionId);
                logger.info("Unregistered player connection: {}", connectionId);
                break;
            case STREAMER:
                streamers.remove(connectionId);
                logger.info("Unregistered streamer connection: {}", connectionId);
                break;
            case SFU:
                sfus.remove(connectionId);
                players.remove(connectionId);
                streamers.remove(connectionId);
                logger.info("Unregistered SFU connection: {}", connectionId);
                break;
        }
    }
    
    /**
     * Gets a player by ID.
     */
    public ConnectionHandler getPlayer(String playerId) {
        return players.get(playerId);
    }
    
    /**
     * Gets a streamer by ID.
     */
    public ConnectionHandler getStreamer(String streamerId) {
        return streamers.get(streamerId);
    }
    
    /**
     * Gets an SFU by ID.
     */
    public ConnectionHandler getSfu(String sfuId) {
        return sfus.get(sfuId);
    }
    
    /**
     * Gets all active players.
     */
    public Collection<ConnectionHandler> getAllPlayers() {
        return new ArrayList<>(players.values());
    }
    
    /**
     * Gets all active streamers.
     */
    public Collection<ConnectionHandler> getAllStreamers() {
        return new ArrayList<>(streamers.values());
    }
    
    /**
     * Gets all active SFUs.
     */
    public Collection<ConnectionHandler> getAllSfus() {
        return new ArrayList<>(sfus.values());
    }
    
    /**
     * Gets all streaming streamer IDs.
     */
    public List<String> getStreamingStreamerIds() {
        return streamers.values().stream()
            .filter(this::isStreaming)
            .map(ConnectionHandler::getConnectionId)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the first available streamer ID.
     */
    public String getFirstStreamerId() {
        return streamers.values().stream()
            .filter(this::isStreaming)
            .map(ConnectionHandler::getConnectionId)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Finds a streamer by ID.
     */
    public ConnectionHandler findStreamer(String streamerId) {
        return streamers.get(streamerId);
    }
    
    /**
     * Gets connection counts for monitoring.
     */
    public Map<String, Integer> getConnectionCounts() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("players", players.size());
        counts.put("streamers", streamers.size());
        counts.put("sfus", sfus.size());
        return counts;
    }
    
    /**
     * Checks if a connection handler is currently streaming.
     * This is a placeholder - actual implementation would check the handler's streaming state.
     */
    private boolean isStreaming(ConnectionHandler handler) {
        // TODO: Implement streaming state check based on handler type
        return true; // For now, assume all streamers are streaming
    }
}