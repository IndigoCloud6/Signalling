package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message containing list of available streamers.
 */
public class StreamerListMessage extends BaseMessage {
    
    @JsonProperty("ids")
    private String[] ids;
    
    public StreamerListMessage() {
        super("streamerList");
    }
    
    public StreamerListMessage(String[] ids) {
        super("streamerList");
        this.ids = ids;
    }
    
    public String[] getIds() {
        return ids;
    }
    
    public void setIds(String[] ids) {
        this.ids = ids;
    }
}