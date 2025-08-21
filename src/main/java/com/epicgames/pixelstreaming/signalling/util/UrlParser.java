package com.epicgames.pixelstreaming.signalling.util;

import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for parsing WebSocket connection URLs and extracting connection attributes.
 * Supports URLs like: ws://127.0.0.1:8888/type=streamer&insid=675ba9d5b1796fc2539d3930&projectid=6821C61B48A925890040C3B3849B06C3
 */
public class UrlParser {
    
    private static final Logger logger = LoggerFactory.getLogger(UrlParser.class);
    
    /**
     * Parses a WebSocket URL path and extracts connection attributes.
     * 
     * @param path The URL path (e.g., "/type=streamer&insid=123&projectid=456")
     * @return ConnectionAttributes object containing parsed attributes
     * @throws IllegalArgumentException if the URL is invalid or missing required type parameter
     */
    public static ConnectionAttributes parseConnectionAttributes(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        
        // Remove leading slash if present
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // Parse query parameters
        Map<String, String> parameters = parseQueryParameters(path);
        
        // Extract type parameter (required)
        String typeStr = parameters.get("type");
        if (typeStr == null || typeStr.isEmpty()) {
            throw new IllegalArgumentException("Missing required 'type' parameter in URL: " + path);
        }
        
        // Build ConnectionAttributes
        ConnectionAttributes.Builder builder = new ConnectionAttributes.Builder()
                .setType(typeStr);
        
        // Extract well-known parameters
        String instanceId = parameters.get("insid");
        if (instanceId != null) {
            builder.setInstanceId(instanceId);
        }
        
        String projectId = parameters.get("projectid");
        if (projectId != null) {
            builder.setProjectId(projectId);
        }
        
        // Add all parameters as additional attributes
        builder.addAttributes(parameters);
        
        ConnectionAttributes attributes = builder.build();
        logger.debug("Parsed connection attributes: {}", attributes);
        
        return attributes;
    }
    
    /**
     * Parses query parameters from a URL path.
     * Supports both '&' separated parameters and URL encoding.
     * 
     * @param path The URL path containing parameters
     * @return Map of parameter names to values
     */
    private static Map<String, String> parseQueryParameters(String path) {
        Map<String, String> parameters = new HashMap<>();
        
        if (path == null || path.isEmpty()) {
            return parameters;
        }
        
        // Split by & to get individual parameters
        String[] pairs = path.split("&");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            
            if (keyValue.length == 2) {
                String key = urlDecode(keyValue[0].trim());
                String value = urlDecode(keyValue[1].trim());
                
                if (!key.isEmpty()) {
                    parameters.put(key, value);
                }
            } else if (keyValue.length == 1) {
                String key = urlDecode(keyValue[0].trim());
                if (!key.isEmpty()) {
                    parameters.put(key, "");
                }
            }
        }
        
        return parameters;
    }
    
    /**
     * URL decodes a string value.
     * 
     * @param value The value to decode
     * @return The decoded value
     */
    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            logger.warn("Failed to URL decode value: {}", value, e);
            return value;
        }
    }
}