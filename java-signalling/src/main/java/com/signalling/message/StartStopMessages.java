package com.signalling.message;

/**
 * Message to start streaming.
 */
public class StartStreamingMessage extends BaseMessage {
    
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