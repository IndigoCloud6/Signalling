package com.example.signalling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PeerDisconnectedMessage extends SignallingMessage {
    private final String peerId;

    @JsonCreator
    public PeerDisconnectedMessage(@JsonProperty("peerId") String peerId) {
        this.peerId = peerId;
    }

    @Override
    public String getType() {
        return "peerDisconnected";
    }

    public String getPeerId() {
        return peerId;
    }
}