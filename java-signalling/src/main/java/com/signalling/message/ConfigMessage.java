package com.signalling.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Configuration message sent to new connections.
 * Contains protocol version and peer connection options.
 */
public class ConfigMessage extends BaseMessage {
    
    @JsonProperty("protocolVersion")
    private String protocolVersion;
    
    @JsonProperty("peerConnectionOptions")
    private Map<String, Object> peerConnectionOptions;
    
    public ConfigMessage() {
        super("config");
    }
    
    public ConfigMessage(String protocolVersion, Map<String, Object> peerConnectionOptions) {
        super("config");
        this.protocolVersion = protocolVersion;
        this.peerConnectionOptions = peerConnectionOptions;
    }
    
    public String getProtocolVersion() {
        return protocolVersion;
    }
    
    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
    
    public Map<String, Object> getPeerConnectionOptions() {
        return peerConnectionOptions;
    }
    
    public void setPeerConnectionOptions(Map<String, Object> peerConnectionOptions) {
        this.peerConnectionOptions = peerConnectionOptions;
    }
}