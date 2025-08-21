package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent when streamer disconnects.
 */
public class StreamerDisconnectedMessage extends BaseMessage {
    
    public StreamerDisconnectedMessage() {
        super("streamerDisconnected");
    }
}