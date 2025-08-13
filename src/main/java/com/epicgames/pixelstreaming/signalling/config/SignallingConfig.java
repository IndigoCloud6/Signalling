// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for the signalling server.
 * Maps values from application.yml to strongly-typed configuration objects.
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "signalling")
@Validated
public class SignallingConfig {

    /**
     * Server configuration
     */
    @NotNull
    private ServerConfig server = new ServerConfig();

    /**
     * Security configuration
     */
    @NotNull
    private SecurityConfig security = new SecurityConfig();

    /**
     * WebSocket configuration
     */
    @NotNull
    private WebSocketConfig websocket = new WebSocketConfig();

    /**
     * HTTP API configuration
     */
    @NotNull
    private ApiConfig api = new ApiConfig();

    // Getters and setters
    public ServerConfig getServer() { return server; }
    public void setServer(ServerConfig server) { this.server = server; }

    public SecurityConfig getSecurity() { return security; }
    public void setSecurity(SecurityConfig security) { this.security = security; }

    public WebSocketConfig getWebsocket() { return websocket; }
    public void setWebsocket(WebSocketConfig websocket) { this.websocket = websocket; }

    public ApiConfig getApi() { return api; }
    public void setApi(ApiConfig api) { this.api = api; }

    /**
     * Server-specific configuration
     */
    public static class ServerConfig {
        @Min(1024)
        @Max(65535)
        private int streamerPort = 8888;

        @Min(1024)
        @Max(65535)
        private int playerPort = 8889;

        @Min(1024)
        @Max(65535)
        private int sfuPort = 8890;

        @Min(1024)
        @Max(65535)
        private int httpPort = 8080;

        @Min(1)
        @Max(10000)
        private int maxSubscribers = 100;

        @NotNull
        private String host = "0.0.0.0";

        private boolean enableSfu = true;

        // Getters and setters
        public int getStreamerPort() { return streamerPort; }
        public void setStreamerPort(int streamerPort) { this.streamerPort = streamerPort; }

        public int getPlayerPort() { return playerPort; }
        public void setPlayerPort(int playerPort) { this.playerPort = playerPort; }

        public int getSfuPort() { return sfuPort; }
        public void setSfuPort(int sfuPort) { this.sfuPort = sfuPort; }

        public int getHttpPort() { return httpPort; }
        public void setHttpPort(int httpPort) { this.httpPort = httpPort; }

        public int getMaxSubscribers() { return maxSubscribers; }
        public void setMaxSubscribers(int maxSubscribers) { this.maxSubscribers = maxSubscribers; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public boolean isEnableSfu() { return enableSfu; }
        public void setEnableSfu(boolean enableSfu) { this.enableSfu = enableSfu; }
    }

    /**
     * Security-specific configuration
     */
    public static class SecurityConfig {
        private boolean enableAuth = false;
        private String authToken = "";

        @Min(1)
        @Max(10000)
        private int rateLimitPerMinute = 60;

        private boolean enableCors = true;
        private String[] allowedOrigins = {"*"};

        // Getters and setters
        public boolean isEnableAuth() { return enableAuth; }
        public void setEnableAuth(boolean enableAuth) { this.enableAuth = enableAuth; }

        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }

        public int getRateLimitPerMinute() { return rateLimitPerMinute; }
        public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

        public boolean isEnableCors() { return enableCors; }
        public void setEnableCors(boolean enableCors) { this.enableCors = enableCors; }

        public String[] getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String[] allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    /**
     * WebSocket-specific configuration
     */
    public static class WebSocketConfig {
        @Min(1024)
        @Max(1048576)
        private int maxFrameSize = 65536;

        @Min(1)
        @Max(3600)
        private int pingIntervalSeconds = 30;

        @Min(1)
        @Max(3600)
        private int connectionTimeoutSeconds = 60;

        // Getters and setters
        public int getMaxFrameSize() { return maxFrameSize; }
        public void setMaxFrameSize(int maxFrameSize) { this.maxFrameSize = maxFrameSize; }

        public int getPingIntervalSeconds() { return pingIntervalSeconds; }
        public void setPingIntervalSeconds(int pingIntervalSeconds) { this.pingIntervalSeconds = pingIntervalSeconds; }

        public int getConnectionTimeoutSeconds() { return connectionTimeoutSeconds; }
        public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) { this.connectionTimeoutSeconds = connectionTimeoutSeconds; }
    }

    /**
     * API-specific configuration
     */
    public static class ApiConfig {
        private boolean enableHealthEndpoint = true;
        private boolean enableStatsEndpoint = true;
        private boolean enableConnectionsEndpoint = true;
        private boolean enableConfigEndpoint = false;

        @NotNull
        private String basePath = "/api";

        // Getters and setters
        public boolean isEnableHealthEndpoint() { return enableHealthEndpoint; }
        public void setEnableHealthEndpoint(boolean enableHealthEndpoint) { this.enableHealthEndpoint = enableHealthEndpoint; }

        public boolean isEnableStatsEndpoint() { return enableStatsEndpoint; }
        public void setEnableStatsEndpoint(boolean enableStatsEndpoint) { this.enableStatsEndpoint = enableStatsEndpoint; }

        public boolean isEnableConnectionsEndpoint() { return enableConnectionsEndpoint; }
        public void setEnableConnectionsEndpoint(boolean enableConnectionsEndpoint) { this.enableConnectionsEndpoint = enableConnectionsEndpoint; }

        public boolean isEnableConfigEndpoint() { return enableConfigEndpoint; }
        public void setEnableConfigEndpoint(boolean enableConfigEndpoint) { this.enableConfigEndpoint = enableConfigEndpoint; }

        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
    }
}