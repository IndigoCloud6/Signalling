// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Utility class for JSON processing and string manipulation.
 * Provides helper methods for formatting and parsing data.
 */
public final class StringUtils {

    private static final Logger logger = LoggerFactory.getLogger(StringUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private StringUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Convert an object to a prettified JSON string.
     *
     * @param obj The object to convert
     * @return The prettified JSON string, or null if conversion fails
     */
    public static String beautify(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to beautify object: {}", obj, e);
            return null;
        }
    }

    /**
     * Convert an object to a compact JSON string.
     *
     * @param obj The object to convert
     * @return The JSON string, or null if conversion fails
     */
    public static String stringify(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to stringify object: {}", obj, e);
            return null;
        }
    }

    /**
     * Parse a JSON string to a JsonNode.
     *
     * @param json The JSON string
     * @return The JsonNode, or null if parsing fails
     */
    public static JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON: {}", json, e);
            return null;
        }
    }

    /**
     * Parse a JSON string to a Map.
     *
     * @param json The JSON string
     * @return The Map, or null if parsing fails
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJsonToMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON to Map: {}", json, e);
            return null;
        }
    }

    /**
     * Check if a string is null or empty.
     *
     * @param str The string to check
     * @return true if the string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if a string is not null and not empty.
     *
     * @param str The string to check
     * @return true if the string is not null and not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Truncate a string to a maximum length.
     *
     * @param str The string to truncate
     * @param maxLength The maximum length
     * @return The truncated string
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Generate a random alphanumeric string.
     *
     * @param length The length of the string
     * @return The random string
     */
    public static String randomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Sanitize a string for logging (remove newlines and control characters).
     *
     * @param str The string to sanitize
     * @return The sanitized string
     */
    public static String sanitizeForLogging(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("[\r\n\t]", " ").replaceAll("\\p{Cntrl}", "");
    }
}