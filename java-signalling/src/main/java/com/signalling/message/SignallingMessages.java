package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent by players to subscribe to a streamer.
 */
class SubscribeMessage extends BaseMessage {
    
    @JsonProperty("streamerId")
    private String streamerId;
    
    public SubscribeMessage() {
        super("subscribe");
    }
    
    public SubscribeMessage(String streamerId) {
        super("subscribe");
        this.streamerId = streamerId;
    }
    
    @Override
    public String getStreamerId() {
        return streamerId;
    }
    
    @Override
    public void setStreamerId(String streamerId) {
        this.streamerId = streamerId;
    }
}

/**
 * Message sent by players to unsubscribe from current streamer.
 */
class UnsubscribeMessage extends BaseMessage {
    
    public UnsubscribeMessage() {
        super("unsubscribe");
    }
}

/**
 * Message sent to streamer when a player connects.
 */
class PlayerConnectedMessage extends BaseMessage {
    
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

/**
 * Message sent to streamer when a player disconnects.
 */
class PlayerDisconnectedMessage extends BaseMessage {
    
    public PlayerDisconnectedMessage() {
        super("playerDisconnected");
    }
    
    public PlayerDisconnectedMessage(String playerId) {
        super("playerDisconnected");
        setPlayerId(playerId);
    }
}

/**
 * Message containing list of available streamers.
 */
class StreamerListMessage extends BaseMessage {
    
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

/**
 * Message requesting list of streamers.
 */
class ListStreamersMessage extends BaseMessage {
    
    public ListStreamersMessage() {
        super("listStreamers");
    }
}

/**
 * Message sent when subscription fails.
 */
class SubscribeFailedMessage extends BaseMessage {
    
    @JsonProperty("message")
    private String message;
    
    public SubscribeFailedMessage() {
        super("subscribeFailed");
    }
    
    public SubscribeFailedMessage(String message) {
        super("subscribeFailed");
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

/**
 * Message sent when streamer ID changes.
 */
class StreamerIdChangedMessage extends BaseMessage {
    
    @JsonProperty("newID")
    private String newId;
    
    public StreamerIdChangedMessage() {
        super("streamerIdChanged");
    }
    
    public StreamerIdChangedMessage(String newId) {
        super("streamerIdChanged");
        this.newId = newId;
    }
    
    public String getNewId() {
        return newId;
    }
    
    public void setNewId(String newId) {
        this.newId = newId;
    }
}

/**
 * Message sent when streamer disconnects.
 */
class StreamerDisconnectedMessage extends BaseMessage {
    
    public StreamerDisconnectedMessage() {
        super("streamerDisconnected");
    }
}