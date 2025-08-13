package com.example.signalling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IceCandidateMessage extends SignallingMessage {
    private final String from;
    private final String to;
    private final IceCandidate candidate;

    @JsonCreator
    public IceCandidateMessage(@JsonProperty("from") String from,
                              @JsonProperty("to") String to,
                              @JsonProperty("candidate") IceCandidate candidate) {
        this.from = from;
        this.to = to;
        this.candidate = candidate;
    }

    @Override
    public String getType() {
        return "iceCandidate";
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public IceCandidate getCandidate() {
        return candidate;
    }

    public static class IceCandidate {
        private final String candidate;
        private final String sdpMid;
        private final int sdpMLineIndex;

        @JsonCreator
        public IceCandidate(@JsonProperty("candidate") String candidate,
                           @JsonProperty("sdpMid") String sdpMid,
                           @JsonProperty("sdpMLineIndex") int sdpMLineIndex) {
            this.candidate = candidate;
            this.sdpMid = sdpMid;
            this.sdpMLineIndex = sdpMLineIndex;
        }

        public String getCandidate() {
            return candidate;
        }

        public String getSdpMid() {
            return sdpMid;
        }

        public int getSdpMLineIndex() {
            return sdpMLineIndex;
        }
    }
}