package com.epicgames.pixelstreaming.signalling.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents attributes parsed from WebSocket connection URLs.
 * Supports parsing URLs like: ws://127.0.0.1:8888/type=streamer&insid=675ba9d5b1796fc2539d3930&projectid=6821C61B48A925890040C3B3849B06C3
 */
public class ConnectionAttributes {
    
    public enum ConnectionType {
        STREAMER("streamer"),
        PLAYER("player"),
        SFU("sfu");
        
        private final String value;
        
        ConnectionType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static ConnectionType fromString(String value) {
            for (ConnectionType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown connection type: " + value);
        }
    }
    
    private final ConnectionType type;
    private final String instanceId;
    private final String projectId;
    private final Map<String, String> additionalAttributes;
    
    private ConnectionAttributes(Builder builder) {
        this.type = builder.type;
        this.instanceId = builder.instanceId;
        this.projectId = builder.projectId;
        this.additionalAttributes = new HashMap<>(builder.additionalAttributes);
    }
    
    public ConnectionType getType() {
        return type;
    }
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public Map<String, String> getAdditionalAttributes() {
        return new HashMap<>(additionalAttributes);
    }
    
    public String getAttribute(String key) {
        return additionalAttributes.get(key);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionAttributes that = (ConnectionAttributes) o;
        return type == that.type &&
               Objects.equals(instanceId, that.instanceId) &&
               Objects.equals(projectId, that.projectId) &&
               Objects.equals(additionalAttributes, that.additionalAttributes);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, instanceId, projectId, additionalAttributes);
    }
    
    @Override
    public String toString() {
        return "ConnectionAttributes{" +
               "type=" + type +
               ", instanceId='" + instanceId + '\'' +
               ", projectId='" + projectId + '\'' +
               ", additionalAttributes=" + additionalAttributes +
               '}';
    }
    
    /**
     * Builder class for creating ConnectionAttributes instances.
     */
    public static class Builder {
        private ConnectionType type;
        private String instanceId;
        private String projectId;
        private Map<String, String> additionalAttributes = new HashMap<>();
        
        public Builder setType(ConnectionType type) {
            this.type = type;
            return this;
        }
        
        public Builder setType(String type) {
            this.type = ConnectionType.fromString(type);
            return this;
        }
        
        public Builder setInstanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }
        
        public Builder setProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }
        
        public Builder addAttribute(String key, String value) {
            this.additionalAttributes.put(key, value);
            return this;
        }
        
        public Builder addAttributes(Map<String, String> attributes) {
            this.additionalAttributes.putAll(attributes);
            return this;
        }
        
        public ConnectionAttributes build() {
            if (type == null) {
                throw new IllegalStateException("Connection type is required");
            }
            return new ConnectionAttributes(this);
        }
    }
}