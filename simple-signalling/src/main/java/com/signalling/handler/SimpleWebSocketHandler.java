package com.signalling.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple WebSocket handler that parses URL parameters and routes connections.
 * Supports type=streamer, type=player, type=sfu URL parameters.
 */
@Component
public class SimpleWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleWebSocketHandler.class);
    
    // Track active sessions and their connection types
    private final Map<String, String> sessionTypes = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            logger.info("New WebSocket connection established: {}", session.getId());
            
            // Parse connection type from URL
            String connectionType = parseConnectionType(session);
            sessionTypes.put(session.getId(), connectionType);
            
            logger.info("Connection {} established as type: {}", session.getId(), connectionType);
            
            // Send welcome message
            String welcomeMsg = String.format("{\"type\":\"welcome\",\"connectionType\":\"%s\",\"sessionId\":\"%s\"}", 
                connectionType, session.getId());
            session.sendMessage(new TextMessage(welcomeMsg));
            
        } catch (Exception e) {
            logger.error("Error establishing connection for session {}: {}", session.getId(), e.getMessage(), e);
            session.close(CloseStatus.BAD_DATA.withReason("Invalid connection parameters"));
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            String connectionType = sessionTypes.get(session.getId());
            logger.debug("Received message from {} ({}): {}", session.getId(), connectionType, message.getPayload());
            
            // Echo the message back for now
            String echoMsg = String.format("{\"type\":\"echo\",\"originalMessage\":%s,\"from\":\"%s\"}", 
                message.getPayload(), connectionType);
            session.sendMessage(new TextMessage(echoMsg));
            
        } catch (Exception e) {
            logger.error("Error handling message from session {}: {}", session.getId(), e.getMessage(), e);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String connectionType = sessionTypes.remove(session.getId());
        logger.info("WebSocket connection closed: {} ({}) with status: {}", 
            session.getId(), connectionType, closeStatus);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Parses connection type from the WebSocket session URL.
     * Supports URLs like: ws://127.0.0.1:8888/signalling?type=streamer&insid=123&projectid=456
     */
    private String parseConnectionType(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            throw new IllegalArgumentException("Session URI is null");
        }
        
        String query = uri.getQuery();
        if (query == null) {
            throw new IllegalArgumentException("Missing query parameters");
        }
        
        Map<String, String> queryParams = UriComponentsBuilder.fromUriString("?" + query)
            .build()
            .getQueryParams()
            .toSingleValueMap();
        
        String type = queryParams.get("type");
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required 'type' parameter");
        }
        
        // Validate connection type
        if (!type.equals("streamer") && !type.equals("player") && !type.equals("sfu")) {
            throw new IllegalArgumentException("Invalid connection type: " + type + 
                ". Valid types are: streamer, player, sfu");
        }
        
        logger.debug("Parsed connection type '{}' with query params: {}", type, queryParams);
        
        return type;
    }
}