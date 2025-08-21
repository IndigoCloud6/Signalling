package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent to streamer when a player disconnects.
 */
public class PlayerDisconnectedMessage extends BaseMessage {
    
    public PlayerDisconnectedMessage() {
        super("playerDisconnected");
    }
    
    public PlayerDisconnectedMessage(String playerId) {
        super("playerDisconnected");
        setPlayerId(playerId);
    }
}