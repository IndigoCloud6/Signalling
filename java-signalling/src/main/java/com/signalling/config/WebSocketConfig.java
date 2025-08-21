package com.signalling.config;

import com.signalling.handler.SignallingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for the signalling server.
 * Configures a single endpoint that handles routing based on URL parameters.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignallingWebSocketHandler signallingHandler;

    @Autowired
    public WebSocketConfig(SignallingWebSocketHandler signallingHandler) {
        this.signallingHandler = signallingHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Single endpoint for all connection types with URL parameter routing
        registry.addHandler(signallingHandler, "/signalling")
                .setAllowedOrigins("*"); // Configure CORS as needed
    }
}