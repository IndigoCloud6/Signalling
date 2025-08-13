// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the Pixel Streaming Signalling Server.
 * This application provides high-performance signalling capabilities for Pixel Streaming
 * using Netty for WebSocket handling and Spring Boot for configuration management.
 */
@SpringBootApplication(exclude = {WebMvcAutoConfiguration.class})
@EnableAsync
public class SignallingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignallingApplication.class, args);
    }
}