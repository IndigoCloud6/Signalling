// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility class for handling message serialization and deserialization.
 * Provides methods to convert between JSON strings and message objects.
 */
@Component
public class MessageHelper {

    private static final Logger logger = LoggerFactory.getLogger(MessageHelper.class);
    private final ObjectMapper objectMapper;

    public MessageHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse a JSON string into a BaseMessage object.
     *
     * @param json The JSON string to parse
     * @return The parsed BaseMessage, or null if parsing fails
     */
    public BaseMessage parseMessage(String json) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            
            if (!rootNode.has("type")) {
                logger.warn("Message missing 'type' field: {}", json);
                return null;
            }

            String type = rootNode.get("type").asText();
            String id = rootNode.has("id") ? rootNode.get("id").asText() : null;
            JsonNode data = rootNode.has("data") ? rootNode.get("data") : null;

            return new GenericMessage(type, id, data);

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse message JSON: {}", json, e);
            return null;
        }
    }

    /**
     * Serialize a BaseMessage to JSON string.
     *
     * @param message The message to serialize
     * @return The JSON string, or null if serialization fails
     */
    public String serializeMessage(BaseMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message: {}", message, e);
            return null;
        }
    }

    /**
     * Create a config message with peer options.
     *
     * @param peerOptions The peer configuration options
     * @return The config message
     */
    public BaseMessage createConfigMessage(JsonNode peerOptions) {
        return new GenericMessage(MessageTypes.CONFIG, null, peerOptions);
    }

    /**
     * Create a player count message.
     *
     * @param count The current player count
     * @return The player count message
     */
    public BaseMessage createPlayerCountMessage(int count) {
        try {
            JsonNode countData = objectMapper.valueToTree(new PlayerCountData(count));
            return new GenericMessage(MessageTypes.PLAYER_COUNT, null, countData);
        } catch (Exception e) {
            logger.error("Failed to create player count message", e);
            return null;
        }
    }

    /**
     * Create an error message.
     *
     * @param errorMessage The error message text
     * @return The error message
     */
    public BaseMessage createErrorMessage(String errorMessage) {
        try {
            JsonNode errorData = objectMapper.valueToTree(new ErrorData(errorMessage));
            return new GenericMessage(MessageTypes.ERROR, null, errorData);
        } catch (Exception e) {
            logger.error("Failed to create error message", e);
            return null;
        }
    }

    /**
     * Create a ping message.
     *
     * @return The ping message
     */
    public BaseMessage createPingMessage() {
        return new GenericMessage(MessageTypes.PING);
    }

    /**
     * Create a pong message.
     *
     * @return The pong message
     */
    public BaseMessage createPongMessage() {
        return new GenericMessage(MessageTypes.PONG);
    }

    /**
     * Generic message implementation.
     */
    private static class GenericMessage extends BaseMessage {
        public GenericMessage(String type) {
            super(type);
        }

        public GenericMessage(String type, String id, JsonNode data) {
            super(type, id, data);
        }
    }

    /**
     * Data class for player count messages.
     */
    private static class PlayerCountData {
        private final int count;

        public PlayerCountData(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Data class for error messages.
     */
    private static class ErrorData {
        private final String message;

        public ErrorData(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}