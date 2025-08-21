package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Streamer data channels message for SFU.
 */
public class StreamerDataChannelsMessage extends BaseMessage {
    
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