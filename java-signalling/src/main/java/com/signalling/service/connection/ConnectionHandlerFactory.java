package com.signalling.service.connection;

import com.signalling.model.ConnectionAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for creating appropriate connection handlers based on connection type.
 * Implements the Factory pattern to create Player, Streamer, or SFU handlers.
 */
@Component
public class ConnectionHandlerFactory {
    
    private final PlayerConnectionHandler playerHandler;
    private final StreamerConnectionHandler streamerHandler;
    private final SfuConnectionHandler sfuHandler;
    
    @Autowired
    public ConnectionHandlerFactory(PlayerConnectionHandler playerHandler,
                                  StreamerConnectionHandler streamerHandler,
                                  SfuConnectionHandler sfuHandler) {
        this.playerHandler = playerHandler;
        this.streamerHandler = streamerHandler;
        this.sfuHandler = sfuHandler;
    }
    
    /**
     * Creates a connection handler based on the connection type.
     * 
     * @param type The type of connection (PLAYER, STREAMER, SFU)
     * @return Appropriate connection handler
     * @throws IllegalArgumentException if connection type is not supported
     */
    public ConnectionHandler createHandler(ConnectionAttributes.ConnectionType type) {
        switch (type) {
            case PLAYER:
                return new PlayerConnectionHandler();
            case STREAMER:
                return new StreamerConnectionHandler();
            case SFU:
                return new SfuConnectionHandler();
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + type);
        }
    }
}