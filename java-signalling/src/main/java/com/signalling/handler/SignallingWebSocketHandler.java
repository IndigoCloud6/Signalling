package com.signalling.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalling.model.ConnectionAttributes;
import com.signalling.service.ConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main WebSocket handler that routes connections based on URL parameters.
 * Supports single-port handling with type-based routing for streamer, player, and SFU connections.
 */
@Component
public class SignallingWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SignallingWebSocketHandler.class);
    
    private final ConnectionService connectionService;
    private final ObjectMapper objectMapper;
    
    // Track sessions and their connection attributes
    private final Map<String, ConnectionAttributes> sessionAttributes = new ConcurrentHashMap<>();
    
    @Autowired
    public SignallingWebSocketHandler(ConnectionService connectionService, ObjectMapper objectMapper) {
        this.connectionService = connectionService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            logger.info("New WebSocket connection established: {}", session.getId());
            
            // Parse connection attributes from URL
            ConnectionAttributes attributes = parseConnectionAttributes(session);
            sessionAttributes.put(session.getId(), attributes);
            
            logger.info("Connection {} parsed as type: {} with attributes: {}", 
                session.getId(), attributes.getType(), attributes);
            
            // Route to appropriate connection handler
            connectionService.handleNewConnection(session, attributes);
            
        } catch (Exception e) {
            logger.error("Error establishing connection for session {}: {}", session.getId(), e.getMessage(), e);
            session.close(CloseStatus.BAD_DATA.withReason("Invalid connection parameters"));
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            ConnectionAttributes attributes = sessionAttributes.get(session.getId());
            if (attributes == null) {
                logger.warn("Received message from unregistered session: {}", session.getId());
                return;
            }
            
            logger.debug("Received message from {}: {}", session.getId(), message.getPayload());
            
            // Forward message to connection service for processing
            connectionService.handleMessage(session, attributes, message);
            
        } catch (Exception e) {
            logger.error("Error handling message from session {}: {}", session.getId(), e.getMessage(), e);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
        
        ConnectionAttributes attributes = sessionAttributes.get(session.getId());
        if (attributes != null) {
            connectionService.handleConnectionError(session, attributes, exception);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSocket connection closed: {} with status: {}", session.getId(), closeStatus);
        
        ConnectionAttributes attributes = sessionAttributes.remove(session.getId());
        if (attributes != null) {
            connectionService.handleConnectionClosed(session, attributes, closeStatus);
        }
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Parses connection attributes from the WebSocket session URL.
     * 
     * @param session WebSocket session
     * @return ConnectionAttributes parsed from URL parameters
     * @throws IllegalArgumentException if required parameters are missing or invalid
     */
    private ConnectionAttributes parseConnectionAttributes(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            throw new IllegalArgumentException("Session URI is null");
        }
        
        Map<String, String> queryParams = new HashMap<>();
        
        // Parse query parameters
        String query = uri.getQuery();
        if (query != null) {
            UriComponentsBuilder.fromUriString("?" + query)
                .build()
                .getQueryParams()
                .forEach((key, values) -> {
                    if (!values.isEmpty()) {
                        queryParams.put(key, values.get(0));
                    }
                });
        }
        
        logger.debug("Parsed query parameters: {}", queryParams);
        
        return ConnectionAttributes.fromQueryParams(queryParams);
    }
}