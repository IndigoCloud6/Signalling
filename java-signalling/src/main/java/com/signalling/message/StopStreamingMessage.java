package com.signalling.message;

/**
 * Message to stop streaming.
 */
public class StopStreamingMessage extends BaseMessage {
    
    public StopStreamingMessage() {
        super("stopStreaming");
    }
}