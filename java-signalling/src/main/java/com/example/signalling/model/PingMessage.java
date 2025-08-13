package com.example.signalling.model;

public class PingMessage extends SignallingMessage {
    @Override
    public String getType() {
        return "ping";
    }
}
