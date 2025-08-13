package com.example.signalling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AnswerMessage extends SignallingMessage {
    private final String fromStreamer;
    private final String toPlayer;
    private final String sdp;

    @JsonCreator
    public AnswerMessage(@JsonProperty("fromStreamer") String fromStreamer,
                        @JsonProperty("toPlayer") String toPlayer,
                        @JsonProperty("sdp") String sdp) {
        this.fromStreamer = fromStreamer;
        this.toPlayer = toPlayer;
        this.sdp = sdp;
    }

    @Override
    public String getType() {
        return "answer";
    }

    public String getFromStreamer() {
        return fromStreamer;
    }

    public String getToPlayer() {
        return toPlayer;
    }

    public String getSdp() {
        return sdp;
    }
}