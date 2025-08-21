package com.signalling.config;

import com.signalling.handler.SimpleWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for the simple signalling server.
 * Uses raw WebSocket handling with URL parameter routing.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SimpleWebSocketHandler webSocketHandler;

    @Autowired
    public WebSocketConfig(SimpleWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Single endpoint for all connection types with URL parameter routing
        registry.addHandler(webSocketHandler, "/signalling")
                .setAllowedOrigins("*"); // Configure CORS as needed
    }
}