package com.epicgames.pixelstreaming.signalling.factory;

import com.epicgames.pixelstreaming.signalling.connection.*;
import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import org.springframework.stereotype.Component;

/**
 * Factory class for creating connection handlers based on connection attributes.
 * Implements the Factory pattern to encapsulate connection handler creation logic.
 */
@Component
public class ConnectionHandlerFactory {
    
    /**
     * Creates a connection handler based on the connection attributes.
     * 
     * @param attributes The connection attributes containing the connection type
     * @param remoteAddress The remote address of the connection
     * @return A connection handler appropriate for the connection type
     * @throws IllegalArgumentException if the connection type is not supported
     */
    public ConnectionHandler createConnectionHandler(ConnectionAttributes attributes, String remoteAddress) {
        switch (attributes.getType()) {
            case STREAMER:
                return new StreamerConnectionHandler(attributes, remoteAddress);
            case PLAYER:
                return new PlayerConnectionHandler(attributes, remoteAddress);
            case SFU:
                return new SFUConnectionHandler(attributes, remoteAddress);
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + attributes.getType());
        }
    }
}