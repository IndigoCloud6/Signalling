package com.signalling.service.connection;

import com.signalling.message.BaseMessage;
import com.signalling.model.ConnectionAttributes;
import com.signalling.registry.ConnectionRegistry;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/**
 * Interface for handling different types of signalling connections.
 * Implements the Strategy pattern for Player, Streamer, and SFU connection handling.
 */
public interface ConnectionHandler {
    
    /**
     * Initializes the connection handler with session and attributes.
     */
    void initialize(WebSocketSession session, ConnectionAttributes attributes, ConnectionRegistry registry);
    
    /**
     * Handles incoming signalling messages.
     */
    void handleMessage(BaseMessage message);
    
    /**
     * Handles connection errors.
     */
    void handleError(Throwable error);
    
    /**
     * Handles connection disconnection and cleanup.
     */
    void handleDisconnection(CloseStatus closeStatus);
    
    /**
     * Sends a message to the WebSocket session.
     */
    void sendMessage(BaseMessage message);
    
    /**
     * Gets the connection type this handler manages.
     */
    ConnectionAttributes.ConnectionType getConnectionType();
    
    /**
     * Gets the unique identifier for this connection.
     */
    String getConnectionId();
    
    /**
     * Gets the WebSocket session.
     */
    WebSocketSession getSession();
    
    /**
     * Gets the connection attributes.
     */
    ConnectionAttributes getAttributes();
}