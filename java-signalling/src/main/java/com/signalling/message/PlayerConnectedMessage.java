package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent to streamer when a player connects.
 */
public class PlayerConnectedMessage extends BaseMessage {
    
    @JsonProperty("dataChannel")
    private boolean dataChannel;
    
    @JsonProperty("sfu")
    private boolean sfu;
    
    public PlayerConnectedMessage() {
        super("playerConnected");
    }
    
    public PlayerConnectedMessage(String playerId, boolean dataChannel, boolean sfu) {
        super("playerConnected");
        setPlayerId(playerId);
        this.dataChannel = dataChannel;
        this.sfu = sfu;
    }
    
    public boolean isDataChannel() {
        return dataChannel;
    }
    
    public void setDataChannel(boolean dataChannel) {
        this.dataChannel = dataChannel;
    }
    
    public boolean isSfu() {
        return sfu;
    }
    
    public void setSfu(boolean sfu) {
        this.sfu = sfu;
    }
}