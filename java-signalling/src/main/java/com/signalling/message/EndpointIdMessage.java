package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Endpoint ID message for streamer identification.
 */
public class EndpointIdMessage extends BaseMessage {
    
    @JsonProperty("id")
    private String id;
    
    public EndpointIdMessage() {
        super("endpointId");
    }
    
    public EndpointIdMessage(String id) {
        super("endpointId");
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
}