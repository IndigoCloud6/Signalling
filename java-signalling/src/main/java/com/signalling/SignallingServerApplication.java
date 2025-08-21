package com.signalling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Main Spring Boot application class for the Java Signalling Server.
 * 
 * This server provides WebSocket signalling capabilities for pixel streaming applications,
 * supporting Player, Streamer, and SFU connection types on a single port with URL-based routing.
 */
@SpringBootApplication
public class SignallingServerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(SignallingServerApplication.class);
    
    public static void main(String[] args) {
        logger.info("Starting Java Signalling Server...");
        
        ConfigurableApplicationContext context = SpringApplication.run(SignallingServerApplication.class, args);
        
        String port = context.getEnvironment().getProperty("server.port", "8888");
        logger.info("Java Signalling Server started successfully on port {}", port);
        logger.info("WebSocket endpoints available:");
        logger.info("  - Streamer: ws://127.0.0.1:{}/signalling?type=streamer", port);
        logger.info("  - Player:   ws://127.0.0.1:{}/signalling?type=player", port);
        logger.info("  - SFU:      ws://127.0.0.1:{}/signalling?type=sfu", port);
    }
}