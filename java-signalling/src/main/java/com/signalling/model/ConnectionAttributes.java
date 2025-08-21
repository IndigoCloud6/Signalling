package com.signalling.model;

import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Represents the attributes extracted from a WebSocket connection URL.
 * Supports parsing URL parameters like type, insid, projectid, etc.
 * 
 * Example URLs:
 * - ws://127.0.0.1:8888/signalling?type=streamer
 * - ws://127.0.0.1:8888/signalling?type=player&insid=675ba9d5b1796fc2539d3930&projectid=6821C61B48A925890040C3B3849B06C3
 */
public class ConnectionAttributes {
    
    public enum ConnectionType {
        STREAMER, PLAYER, SFU
    }
    
    private final ConnectionType type;
    private final String instanceId;
    private final String projectId;
    private final Map<String, String> additionalAttributes;
    
    public ConnectionAttributes(ConnectionType type, String instanceId, String projectId, Map<String, String> additionalAttributes) {
        this.type = Objects.requireNonNull(type, "Connection type cannot be null");
        this.instanceId = instanceId;
        this.projectId = projectId;
        this.additionalAttributes = additionalAttributes != null ? new HashMap<>(additionalAttributes) : new HashMap<>();
    }
    
    /**
     * Creates ConnectionAttributes from URL query parameters.
     * 
     * @param queryParams Map of query parameters from the WebSocket handshake
     * @return ConnectionAttributes object
     * @throws IllegalArgumentException if type parameter is missing or invalid
     */
    public static ConnectionAttributes fromQueryParams(Map<String, String> queryParams) {
        String typeParam = queryParams.get("type");
        if (!StringUtils.hasText(typeParam)) {
            throw new IllegalArgumentException("Missing required 'type' parameter");
        }
        
        ConnectionType type;
        try {
            type = ConnectionType.valueOf(typeParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid connection type: " + typeParam + 
                ". Valid types are: streamer, player, sfu");
        }
        
        String instanceId = queryParams.get("insid");
        String projectId = queryParams.get("projectid");
        
        // Extract additional attributes (excluding the well-known ones)
        Map<String, String> additionalAttributes = new HashMap<>();
        queryParams.forEach((key, value) -> {
            if (!"type".equals(key) && !"insid".equals(key) && !"projectid".equals(key)) {
                additionalAttributes.put(key, value);
            }
        });
        
        return new ConnectionAttributes(type, instanceId, projectId, additionalAttributes);
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
    
    public boolean hasInstanceId() {
        return StringUtils.hasText(instanceId);
    }
    
    public boolean hasProjectId() {
        return StringUtils.hasText(projectId);
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
}