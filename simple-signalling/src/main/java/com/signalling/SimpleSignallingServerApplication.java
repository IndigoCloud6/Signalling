package com.signalling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simple Spring Boot signalling server for testing.
 */
@SpringBootApplication
public class SimpleSignallingServerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleSignallingServerApplication.class);
    
    public static void main(String[] args) {
        logger.info("Starting Simple Signalling Server...");
        
        SpringApplication.run(SimpleSignallingServerApplication.class, args);
        
        logger.info("Simple Signalling Server started successfully on port 8888");
        logger.info("WebSocket endpoint: ws://127.0.0.1:8888/signalling");
    }
}