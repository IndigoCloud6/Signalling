package com.epicgames.pixelstreaming.signalling.registry;

import com.epicgames.pixelstreaming.signalling.connection.ConnectionHandler;
import com.epicgames.pixelstreaming.signalling.connection.PlayerConnectionHandler;
import com.epicgames.pixelstreaming.signalling.connection.SFUConnectionHandler;
import com.epicgames.pixelstreaming.signalling.connection.StreamerConnectionHandler;
import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Registry for managing all active connections.
 * Implements the Observer pattern to notify listeners of connection changes.
 */
@Component
public class ConnectionRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionRegistry.class);
    
    private final Map<String, ConnectionHandler> connections = new ConcurrentHashMap<>();
    private final List<ConnectionRegistryListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Adds a listener to receive connection registry events.
     * 
     * @param listener The listener to add
     */
    public void addListener(ConnectionRegistryListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a listener from receiving connection registry events.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(ConnectionRegistryListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Registers a new connection.
     * 
     * @param connectionHandler The connection handler to register
     */
    public void registerConnection(ConnectionHandler connectionHandler) {
        String connectionId = connectionHandler.getConnectionId();
        connections.put(connectionId, connectionHandler);
        
        logger.info("Registered connection: {} {} from {}", 
                   connectionHandler.getAttributes().getType(),
                   connectionId,
                   connectionHandler.getRemoteAddress());
        
        // Notify listeners
        listeners.forEach(listener -> {
            try {
                listener.onConnectionAdded(connectionHandler);
            } catch (Exception e) {
                logger.error("Error notifying listener of connection addition", e);
            }
        });
    }
    
    /**
     * Unregisters a connection.
     * 
     * @param connectionHandler The connection handler to unregister
     */
    public void unregisterConnection(ConnectionHandler connectionHandler) {
        String connectionId = connectionHandler.getConnectionId();
        ConnectionHandler removed = connections.remove(connectionId);
        
        if (removed != null) {
            logger.info("Unregistered connection: {} {} from {}", 
                       connectionHandler.getAttributes().getType(),
                       connectionId,
                       connectionHandler.getRemoteAddress());
            
            // Notify listeners
            listeners.forEach(listener -> {
                try {
                    listener.onConnectionRemoved(connectionHandler);
                } catch (Exception e) {
                    logger.error("Error notifying listener of connection removal", e);
                }
            });
        }
    }
    
    /**
     * Gets a connection by its ID.
     * 
     * @param connectionId The connection ID
     * @return The connection handler, or null if not found
     */
    public ConnectionHandler getConnection(String connectionId) {
        return connections.get(connectionId);
    }
    
    /**
     * Gets all connections of a specific type.
     * 
     * @param type The connection type to filter by
     * @return List of connection handlers of the specified type
     */
    public List<ConnectionHandler> getConnectionsByType(ConnectionAttributes.ConnectionType type) {
        return connections.values().stream()
                .filter(handler -> handler.getAttributes().getType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all streamer connections that are currently streaming.
     * 
     * @return List of active streamer connection handlers
     */
    public List<StreamerConnectionHandler> getActiveStreamers() {
        return connections.values().stream()
                .filter(handler -> handler instanceof StreamerConnectionHandler)
                .map(handler -> (StreamerConnectionHandler) handler)
                .filter(StreamerConnectionHandler::isStreaming)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all SFU connections that are currently streaming.
     * 
     * @return List of active SFU connection handlers
     */
    public List<SFUConnectionHandler> getActiveSFUs() {
        return connections.values().stream()
                .filter(handler -> handler instanceof SFUConnectionHandler)
                .map(handler -> (SFUConnectionHandler) handler)
                .filter(SFUConnectionHandler::isStreaming)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all player connections subscribed to a specific streamer.
     * 
     * @param streamerId The streamer ID to search for
     * @return List of player connection handlers subscribed to the streamer
     */
    public List<PlayerConnectionHandler> getPlayersSubscribedTo(String streamerId) {
        return connections.values().stream()
                .filter(handler -> handler instanceof PlayerConnectionHandler)
                .map(handler -> (PlayerConnectionHandler) handler)
                .filter(handler -> streamerId.equals(handler.getSubscribedStreamerId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all SFU connections subscribed to a specific streamer.
     * 
     * @param streamerId The streamer ID to search for  
     * @return List of SFU connection handlers subscribed to the streamer
     */
    public List<SFUConnectionHandler> getSFUsSubscribedTo(String streamerId) {
        return connections.values().stream()
                .filter(handler -> handler instanceof SFUConnectionHandler)
                .map(handler -> (SFUConnectionHandler) handler)
                .filter(handler -> streamerId.equals(handler.getSubscribedStreamerId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all active connections.
     * 
     * @return Collection of all connection handlers
     */
    public Collection<ConnectionHandler> getAllConnections() {
        return new ArrayList<>(connections.values());
    }
    
    /**
     * Gets the total number of registered connections.
     * 
     * @return The number of registered connections
     */
    public int getConnectionCount() {
        return connections.size();
    }
    
    /**
     * Gets the count of connections by type.
     * 
     * @param type The connection type to count
     * @return The number of connections of the specified type
     */
    public int getConnectionCount(ConnectionAttributes.ConnectionType type) {
        return (int) connections.values().stream()
                .filter(handler -> handler.getAttributes().getType() == type)
                .count();
    }
    
    /**
     * Checks if there are any connections of a specific type.
     * 
     * @param type The connection type to check
     * @return true if there are connections of the specified type, false otherwise
     */
    public boolean hasConnectionsOfType(ConnectionAttributes.ConnectionType type) {
        return connections.values().stream()
                .anyMatch(handler -> handler.getAttributes().getType() == type);
    }
}