package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message requesting list of streamers.
 */
public class ListStreamersMessage extends BaseMessage {
    
    public ListStreamersMessage() {
        super("listStreamers");
    }
}