package com.epicgames.pixelstreaming.signalling.registry;

import com.epicgames.pixelstreaming.signalling.connection.ConnectionHandler;

/**
 * Interface for listening to connection registry events.
 * Implements the Observer pattern for connection lifecycle management.
 */
public interface ConnectionRegistryListener {
    
    /**
     * Called when a connection is added to the registry.
     * 
     * @param connectionHandler The connection handler that was added
     */
    void onConnectionAdded(ConnectionHandler connectionHandler);
    
    /**
     * Called when a connection is removed from the registry.
     * 
     * @param connectionHandler The connection handler that was removed
     */
    void onConnectionRemoved(ConnectionHandler connectionHandler);
}