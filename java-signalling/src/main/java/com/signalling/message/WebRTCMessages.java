package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ping message for connection health checks.
 */
public class PingMessage extends BaseMessage {
    
    @JsonProperty("time")
    private long time;
    
    public PingMessage() {
        super("ping");
    }
    
    public PingMessage(long time) {
        super("ping");
        this.time = time;
    }
    
    public long getTime() {
        return time;
    }
    
    public void setTime(long time) {
        this.time = time;
    }
}

/**
 * Pong response to ping message.
 */
class PongMessage extends BaseMessage {
    
    @JsonProperty("time")
    private long time;
    
    public PongMessage() {
        super("pong");
    }
    
    public PongMessage(long time) {
        super("pong");
        this.time = time;
    }
    
    public long getTime() {
        return time;
    }
    
    public void setTime(long time) {
        this.time = time;
    }
}

/**
 * WebRTC offer message.
 */
class OfferMessage extends BaseMessage {
    
    @JsonProperty("sdp")
    private String sdp;
    
    public OfferMessage() {
        super("offer");
    }
    
    public OfferMessage(String sdp) {
        super("offer");
        this.sdp = sdp;
    }
    
    public String getSdp() {
        return sdp;
    }
    
    public void setSdp(String sdp) {
        this.sdp = sdp;
    }
}

/**
 * WebRTC answer message.
 */
class AnswerMessage extends BaseMessage {
    
    @JsonProperty("sdp")
    private String sdp;
    
    public AnswerMessage() {
        super("answer");
    }
    
    public AnswerMessage(String sdp) {
        super("answer");
        this.sdp = sdp;
    }
    
    public String getSdp() {
        return sdp;
    }
    
    public void setSdp(String sdp) {
        this.sdp = sdp;
    }
}

/**
 * WebRTC ICE candidate message.
 */
class IceCandidateMessage extends BaseMessage {
    
    @JsonProperty("candidate")
    private String candidate;
    
    @JsonProperty("sdpMid")
    private String sdpMid;
    
    @JsonProperty("sdpMLineIndex")
    private Integer sdpMLineIndex;
    
    public IceCandidateMessage() {
        super("iceCandidate");
    }
    
    public String getCandidate() {
        return candidate;
    }
    
    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }
    
    public String getSdpMid() {
        return sdpMid;
    }
    
    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }
    
    public Integer getSdpMLineIndex() {
        return sdpMLineIndex;
    }
    
    public void setSdpMLineIndex(Integer sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }
}