package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message to disconnect a specific player.
 */
public class DisconnectPlayerMessage extends BaseMessage {
    
    @JsonProperty("reason")
    private String reason;
    
    public DisconnectPlayerMessage() {
        super("disconnectPlayer");
    }
    
    public DisconnectPlayerMessage(String playerId, String reason) {
        super("disconnectPlayer");
        setPlayerId(playerId);
        this.reason = reason;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}