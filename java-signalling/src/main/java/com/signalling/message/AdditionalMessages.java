package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data channel request message.
 */
class DataChannelRequestMessage extends BaseMessage {
    
    public DataChannelRequestMessage() {
        super("dataChannelRequest");
    }
}

/**
 * Message indicating peer data channels are ready.
 */
class PeerDataChannelsReadyMessage extends BaseMessage {
    
    public PeerDataChannelsReadyMessage() {
        super("peerDataChannelsReady");
    }
}

/**
 * Layer preference message for streaming quality control.
 */
class LayerPreferenceMessage extends BaseMessage {
    
    @JsonProperty("spatialLayer")
    private Integer spatialLayer;
    
    @JsonProperty("temporalLayer") 
    private Integer temporalLayer;
    
    public LayerPreferenceMessage() {
        super("layerPreference");
    }
    
    public Integer getSpatialLayer() {
        return spatialLayer;
    }
    
    public void setSpatialLayer(Integer spatialLayer) {
        this.spatialLayer = spatialLayer;
    }
    
    public Integer getTemporalLayer() {
        return temporalLayer;
    }
    
    public void setTemporalLayer(Integer temporalLayer) {
        this.temporalLayer = temporalLayer;
    }
}

/**
 * Endpoint ID message for streamer identification.
 */
class EndpointIdMessage extends BaseMessage {
    
    @JsonProperty("id")
    private String id;
    
    public EndpointIdMessage() {
        super("endpointId");
    }
    
    public EndpointIdMessage(String id) {
        super("endpointId");
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
}

/**
 * Message to disconnect a specific player.
 */
class DisconnectPlayerMessage extends BaseMessage {
    
    @JsonProperty("reason")
    private String reason;
    
    public DisconnectPlayerMessage() {
        super("disconnectPlayer");
    }
    
    public DisconnectPlayerMessage(String playerId, String reason) {
        super("disconnectPlayer");
        setPlayerId(playerId);
        this.reason = reason;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}

/**
 * Message to start streaming.
 */
class StartStreamingMessage extends BaseMessage {
    
    public StartStreamingMessage() {
        super("startStreaming");
    }
}

/**
 * Message to stop streaming.
 */
class StopStreamingMessage extends BaseMessage {
    
    public StopStreamingMessage() {
        super("stopStreaming");
    }
}

/**
 * Streamer data channels message for SFU.
 */
class StreamerDataChannelsMessage extends BaseMessage {
    
    @JsonProperty("sfuId")
    private String sfuId;
    
    public StreamerDataChannelsMessage() {
        super("streamerDataChannels");
    }
    
    public String getSfuId() {
        return sfuId;
    }
    
    public void setSfuId(String sfuId) {
        this.sfuId = sfuId;
    }
}

/**
 * Peer data channels message.
 */
class PeerDataChannelsMessage extends BaseMessage {
    
    public PeerDataChannelsMessage() {
        super("peerDataChannels");
    }
}