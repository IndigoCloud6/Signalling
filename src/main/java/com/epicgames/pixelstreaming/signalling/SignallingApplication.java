package com.epicgames.pixelstreaming.signalling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the Pixel Streaming Signalling Server.
 * This application provides WebSocket signalling for Pixel Streaming using Java 17,
 * Spring Boot 3, and Netty.
 */
@SpringBootApplication
public class SignallingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignallingApplication.class, args);
    }
}