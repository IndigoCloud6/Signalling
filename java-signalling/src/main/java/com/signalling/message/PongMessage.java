package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pong response to ping message.
 */
public class PongMessage extends BaseMessage {
    
    @JsonProperty("time")
    private long time;
    
    public PongMessage() {
        super("pong");
    }
    
    public PongMessage(long time) {
        super("pong");
        this.time = time;
    }
    
    public long getTime() {
        return time;
    }
    
    public void setTime(long time) {
        this.time = time;
    }
}