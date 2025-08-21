package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent when subscription fails.
 */
public class SubscribeFailedMessage extends BaseMessage {
    
    @JsonProperty("message")
    private String message;
    
    public SubscribeFailedMessage() {
        super("subscribeFailed");
    }
    
    public SubscribeFailedMessage(String message) {
        super("subscribeFailed");
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}