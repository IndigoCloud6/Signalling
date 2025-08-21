package com.epicgames.pixelstreaming.signalling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the signalling server.
 */
@Component
@ConfigurationProperties(prefix = "signalling")
public class SignallingProperties {
    
    private Server server = new Server();
    private Protocol protocol = new Protocol();
    
    public Server getServer() {
        return server;
    }
    
    public void setServer(Server server) {
        this.server = server;
    }
    
    public Protocol getProtocol() {
        return protocol;
    }
    
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }
    
    public static class Server {
        private int port = 8888;
        private int maxSubscribers = 100;
        private boolean corsEnabled = true;
        private String corsOrigin = "*";
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public int getMaxSubscribers() {
            return maxSubscribers;
        }
        
        public void setMaxSubscribers(int maxSubscribers) {
            this.maxSubscribers = maxSubscribers;
        }
        
        public boolean isCorsEnabled() {
            return corsEnabled;
        }
        
        public void setCorsEnabled(boolean corsEnabled) {
            this.corsEnabled = corsEnabled;
        }
        
        public String getCorsOrigin() {
            return corsOrigin;
        }
        
        public void setCorsOrigin(String corsOrigin) {
            this.corsOrigin = corsOrigin;
        }
    }
    
    public static class Protocol {
        private String version = "1.0";
        private long pingIntervalMs = 30000;
        private long connectionTimeoutMs = 60000;
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public long getPingIntervalMs() {
            return pingIntervalMs;
        }
        
        public void setPingIntervalMs(long pingIntervalMs) {
            this.pingIntervalMs = pingIntervalMs;
        }
        
        public long getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }
        
        public void setConnectionTimeoutMs(long connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }
    }
}