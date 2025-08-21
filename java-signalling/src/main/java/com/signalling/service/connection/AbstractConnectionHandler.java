package com.signalling.service.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalling.message.BaseMessage;
import com.signalling.message.ConfigMessage;
import com.signalling.model.ConnectionAttributes;
import com.signalling.registry.ConnectionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for connection handlers.
 * Provides common functionality for all connection types.
 */
public abstract class AbstractConnectionHandler implements ConnectionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractConnectionHandler.class);
    
    protected WebSocketSession session;
    protected ConnectionAttributes attributes;
    protected ConnectionRegistry registry;
    protected String connectionId;
    protected final ObjectMapper objectMapper = new ObjectMapper();
    
    // Track message handlers for this connection type
    protected final Map<String, MessageHandler> messageHandlers = new ConcurrentHashMap<>();
    
    @Override
    public void initialize(WebSocketSession session, ConnectionAttributes attributes, ConnectionRegistry registry) {
        this.session = session;
        this.attributes = attributes;
        this.registry = registry;
        this.connectionId = generateConnectionId();
        
        logger.info("Initializing {} connection with ID: {}", getConnectionType(), connectionId);
        
        // Register message handlers
        registerMessageHandlers();
        
        // Register with registry
        registry.register(this);
        
        // Send initial configuration
        sendConfigurationMessage();
        
        // Perform connection-specific initialization
        onConnectionEstablished();
    }
    
    @Override
    public void handleMessage(BaseMessage message) {
        try {
            logger.debug("Processing message type '{}' for connection {}", message.getType(), connectionId);
            
            MessageHandler handler = messageHandlers.get(message.getType());
            if (handler != null) {
                handler.handle(message);
            } else {
                logger.warn("No handler found for message type '{}' on connection {}", message.getType(), connectionId);
                onUnhandledMessage(message);
            }
        } catch (Exception e) {
            logger.error("Error processing message type '{}' for connection {}: {}", 
                message.getType(), connectionId, e.getMessage(), e);
        }
    }
    
    @Override
    public void handleError(Throwable error) {
        logger.error("Connection error for {}: {}", connectionId, error.getMessage(), error);
    }
    
    @Override
    public void handleDisconnection(CloseStatus closeStatus) {
        logger.info("Connection {} disconnecting with status: {}", connectionId, closeStatus);
        
        // Unregister from registry
        registry.unregister(this);
        
        // Perform connection-specific cleanup
        onDisconnection(closeStatus);
    }
    
    @Override
    public void sendMessage(BaseMessage message) {
        if (session == null || !session.isOpen()) {
            logger.warn("Cannot send message to closed connection: {}", connectionId);
            return;
        }
        
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            logger.debug("Sent message type '{}' to connection {}", message.getType(), connectionId);
        } catch (IOException e) {
            logger.error("Failed to send message to connection {}: {}", connectionId, e.getMessage(), e);
        }
    }
    
    @Override
    public String getConnectionId() {
        return connectionId;
    }
    
    @Override
    public WebSocketSession getSession() {
        return session;
    }
    
    @Override
    public ConnectionAttributes getAttributes() {
        return attributes;
    }
    
    /**
     * Generates a unique connection ID.
     */
    protected String generateConnectionId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Sends initial configuration message to the connection.
     */
    protected void sendConfigurationMessage() {
        Map<String, Object> peerOptions = new HashMap<>();
        // Add default peer connection options as needed
        
        ConfigMessage config = new ConfigMessage("1.0", peerOptions);
        sendMessage(config);
    }
    
    /**
     * Registers message handlers specific to this connection type.
     * Must be implemented by subclasses.
     */
    protected abstract void registerMessageHandlers();
    
    /**
     * Called when the connection is successfully established.
     * Can be overridden by subclasses for specific initialization.
     */
    protected void onConnectionEstablished() {
        // Default implementation does nothing
    }
    
    /**
     * Called when the connection is being disconnected.
     * Can be overridden by subclasses for specific cleanup.
     */
    protected void onDisconnection(CloseStatus closeStatus) {
        // Default implementation does nothing
    }
    
    /**
     * Called when an unhandled message is received.
     * Can be overridden by subclasses for specific handling.
     */
    protected void onUnhandledMessage(BaseMessage message) {
        logger.warn("Unhandled message type '{}' for connection {}", message.getType(), connectionId);
    }
    
    /**
     * Helper method to register a message handler.
     */
    protected void registerHandler(String messageType, MessageHandler handler) {
        messageHandlers.put(messageType, handler);
    }
    
    /**
     * Functional interface for message handlers.
     */
    @FunctionalInterface
    protected interface MessageHandler {
        void handle(BaseMessage message) throws Exception;
    }
}