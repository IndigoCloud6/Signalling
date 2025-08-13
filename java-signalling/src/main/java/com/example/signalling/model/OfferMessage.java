package com.example.signalling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OfferMessage extends SignallingMessage {
    private final String fromPlayer;
    private final String toStreamer;
    private final String sdp;

    @JsonCreator
    public OfferMessage(@JsonProperty("fromPlayer") String fromPlayer,
                       @JsonProperty("toStreamer") String toStreamer,
                       @JsonProperty("sdp") String sdp) {
        this.fromPlayer = fromPlayer;
        this.toStreamer = toStreamer;
        this.sdp = sdp;
    }

    @Override
    public String getType() {
        return "offer";
    }

    public String getFromPlayer() {
        return fromPlayer;
    }

    public String getToStreamer() {
        return toStreamer;
    }

    public String getSdp() {
        return sdp;
    }
}