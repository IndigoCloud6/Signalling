package com.example.signalling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterStreamerMessage extends SignallingMessage {
    private final String streamerId;

    @JsonCreator
    public RegisterStreamerMessage(@JsonProperty("streamerId") String streamerId) {
        this.streamerId = streamerId;
    }

    @Override
    public String getType() {
        return "registerStreamer";
    }

    public String getStreamerId() {
        return streamerId;
    }
}