package com.example.signalling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DisconnectPlayerMessage extends SignallingMessage {
    private final String playerId;

    @JsonCreator
    public DisconnectPlayerMessage(@JsonProperty("playerId") String playerId) {
        this.playerId = playerId;
    }

    @Override
    public String getType() {
        return "disconnectPlayer";
    }

    public String getPlayerId() {
        return playerId;
    }
}
