package com.example.signalling.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RegisterStreamerMessage.class, name = "registerStreamer"),
    @JsonSubTypes.Type(value = RegisterPlayerMessage.class, name = "registerPlayer"),
    @JsonSubTypes.Type(value = OfferMessage.class, name = "offer"),
    @JsonSubTypes.Type(value = AnswerMessage.class, name = "answer"),
    @JsonSubTypes.Type(value = IceCandidateMessage.class, name = "iceCandidate"),
    @JsonSubTypes.Type(value = PingMessage.class, name = "ping"),
    @JsonSubTypes.Type(value = DisconnectPlayerMessage.class, name = "disconnectPlayer"),
    @JsonSubTypes.Type(value = ErrorMessage.class, name = "error"),
    @JsonSubTypes.Type(value = PeerDisconnectedMessage.class, name = "peerDisconnected")
})
public abstract class SignallingMessage {
    public abstract String getType();
}