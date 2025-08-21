package com.signalling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalling.message.BaseMessage;
import com.signalling.model.ConnectionAttributes;
import com.signalling.registry.ConnectionRegistry;
import com.signalling.service.connection.ConnectionHandler;
import com.signalling.service.connection.ConnectionHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service that manages WebSocket connections and routes messages to appropriate handlers.
 * Uses the Strategy pattern to handle different connection types (Player, Streamer, SFU).
 */
@Service
public class ConnectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);
    
    private final ConnectionHandlerFactory handlerFactory;
    private final ConnectionRegistry connectionRegistry;
    private final ObjectMapper objectMapper;
    
    // Map to track active connection handlers by session ID
    private final Map<String, ConnectionHandler> activeHandlers = new ConcurrentHashMap<>();
    
    @Autowired
    public ConnectionService(ConnectionHandlerFactory handlerFactory, 
                           ConnectionRegistry connectionRegistry,
                           ObjectMapper objectMapper) {
        this.handlerFactory = handlerFactory;
        this.connectionRegistry = connectionRegistry;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handles new WebSocket connection establishment.
     */
    public void handleNewConnection(WebSocketSession session, ConnectionAttributes attributes) {
        try {
            logger.info("Creating connection handler for session {} with type {}", 
                session.getId(), attributes.getType());
            
            // Create appropriate handler based on connection type
            ConnectionHandler handler = handlerFactory.createHandler(attributes.getType());
            activeHandlers.put(session.getId(), handler);
            
            // Initialize the connection
            handler.initialize(session, attributes, connectionRegistry);
            
            logger.info("Successfully initialized {} connection for session {}", 
                attributes.getType(), session.getId());
                
        } catch (Exception e) {
            logger.error("Failed to handle new connection for session {}: {}", 
                session.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to initialize connection", e);
        }
    }
    
    /**
     * Handles incoming messages from WebSocket sessions.
     */
    public void handleMessage(WebSocketSession session, ConnectionAttributes attributes, 
                            WebSocketMessage<?> message) {
        ConnectionHandler handler = activeHandlers.get(session.getId());
        if (handler == null) {
            logger.warn("No handler found for session {}", session.getId());
            return;
        }
        
        try {
            // Parse JSON message
            String payload = message.getPayload().toString();
            BaseMessage baseMessage = objectMapper.readValue(payload, BaseMessage.class);
            
            logger.debug("Parsed message type '{}' from session {}", 
                baseMessage.getType(), session.getId());
            
            // Handle the message through the appropriate handler
            handler.handleMessage(baseMessage);
            
        } catch (Exception e) {
            logger.error("Error processing message from session {}: {}", 
                session.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Handles WebSocket connection errors.
     */
    public void handleConnectionError(WebSocketSession session, ConnectionAttributes attributes, 
                                    Throwable error) {
        ConnectionHandler handler = activeHandlers.get(session.getId());
        if (handler != null) {
            try {
                handler.handleError(error);
            } catch (Exception e) {
                logger.error("Error in connection error handler for session {}: {}", 
                    session.getId(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Handles WebSocket connection closure.
     */
    public void handleConnectionClosed(WebSocketSession session, ConnectionAttributes attributes, 
                                     CloseStatus closeStatus) {
        ConnectionHandler handler = activeHandlers.remove(session.getId());
        if (handler != null) {
            try {
                handler.handleDisconnection(closeStatus);
                logger.info("Connection handler cleaned up for session {}", session.getId());
            } catch (Exception e) {
                logger.error("Error during connection cleanup for session {}: {}", 
                    session.getId(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Gets the number of active connections.
     */
    public int getActiveConnectionCount() {
        return activeHandlers.size();
    }
    
    /**
     * Gets the connection handler for a session (for testing purposes).
     */
    public ConnectionHandler getHandler(String sessionId) {
        return activeHandlers.get(sessionId);
    }
}