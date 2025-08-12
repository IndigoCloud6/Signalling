// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base class for all signalling messages.
 * Represents the common structure of messages in the Pixel Streaming protocol.
 */
public abstract class BaseMessage {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("data")
    private JsonNode data;

    public BaseMessage() {}

    public BaseMessage(String type) {
        this.type = type;
    }

    public BaseMessage(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public BaseMessage(String type, String id, JsonNode data) {
        this.type = type;
        this.id = id;
        this.data = data;
    }

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public JsonNode getData() { return data; }
    public void setData(JsonNode data) { this.data = data; }

    @Override
    public String toString() {
        return "BaseMessage{" +
                "type='" + type + '\'' +
                ", id='" + id + '\'' +
                ", data=" + data +
                '}';
    }
}