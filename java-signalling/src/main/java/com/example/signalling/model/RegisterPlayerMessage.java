package com.example.signalling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterPlayerMessage extends SignallingMessage {
    private final String playerId;
    private final String streamerId;

    @JsonCreator
    public RegisterPlayerMessage(@JsonProperty("playerId") String playerId,
                                @JsonProperty("streamerId") String streamerId) {
        this.playerId = playerId;
        this.streamerId = streamerId;
    }

    @Override
    public String getType() {
        return "registerPlayer";
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getStreamerId() {
        return streamerId;
    }
}