package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent by players to unsubscribe from current streamer.
 */
public class UnsubscribeMessage extends BaseMessage {
    
    public UnsubscribeMessage() {
        super("unsubscribe");
    }
}