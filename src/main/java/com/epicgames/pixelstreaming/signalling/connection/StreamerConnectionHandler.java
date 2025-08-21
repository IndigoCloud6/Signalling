package com.epicgames.pixelstreaming.signalling.connection;

import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelHandlerContext;

/**
 * Connection handler for streamer connections.
 * Streamers are the sources of media streams that players can subscribe to.
 */
public class StreamerConnectionHandler extends AbstractConnectionHandler {
    
    private boolean streaming = false;
    private int maxSubscribers = 0;
    
    public StreamerConnectionHandler(ConnectionAttributes attributes, String remoteAddress) {
        super(attributes, remoteAddress);
    }
    
    @Override
    public void onConnectionEstablished(ChannelHandlerContext ctx) {
        super.onConnectionEstablished(ctx);
        
        // Send initial configuration to streamer
        sendConfigMessage(ctx);
        
        // Request streamer to identify itself
        sendIdentifyMessage(ctx);
    }
    
    @Override
    protected void handleMessage(ChannelHandlerContext ctx, JsonNode message) {
        String messageType = message.path("type").asText();
        
        switch (messageType) {
            case "endpointId":
                handleEndpointId(ctx, message);
                break;
            case "ping":
                handlePing(ctx, message);
                break;
            case "offer":
            case "answer":
            case "iceCandidate":
                handleMediaMessage(ctx, message);
                break;
            case "disconnectPlayer":
                handleDisconnectPlayer(ctx, message);
                break;
            default:
                logger.warn("Unknown message type from streamer {}: {}", getConnectionId(), messageType);
        }
    }
    
    private void handleEndpointId(ChannelHandlerContext ctx, JsonNode message) {
        String streamerId = message.path("id").asText();
        logger.info("Streamer {} identified as: {}", getConnectionId(), streamerId);
        
        // Set streaming state to true
        streaming = true;
        
        // Send confirmation back to streamer
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "endpointIdConfirm");
        response.put("committedId", streamerId);
        sendMessage(ctx, response);
    }
    
    private void handlePing(ChannelHandlerContext ctx, JsonNode message) {
        ObjectNode pong = objectMapper.createObjectNode();
        pong.put("type", "pong");
        if (message.has("time")) {
            pong.put("time", message.get("time").asLong());
        }
        sendMessage(ctx, pong);
    }
    
    private void handleMediaMessage(ChannelHandlerContext ctx, JsonNode message) {
        // Forward media messages to the appropriate player
        String playerId = message.path("playerId").asText();
        if (!playerId.isEmpty()) {
            logger.debug("Forwarding media message from streamer {} to player {}", 
                        getConnectionId(), playerId);
            // TODO: Implement message forwarding through message router
        } else {
            logger.warn("Media message from streamer {} missing playerId", getConnectionId());
        }
    }
    
    private void handleDisconnectPlayer(ChannelHandlerContext ctx, JsonNode message) {
        String playerId = message.path("playerId").asText();
        String reason = message.path("reason").asText("No reason provided");
        
        logger.info("Streamer {} requested disconnect of player {}: {}", 
                   getConnectionId(), playerId, reason);
        
        // TODO: Implement player disconnection through connection registry
    }
    
    private void sendConfigMessage(ChannelHandlerContext ctx) {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("type", "config");
        config.put("protocolVersion", "1.0");
        
        ObjectNode peerConnectionOptions = objectMapper.createObjectNode();
        // Add default peer connection options here
        config.set("peerConnectionOptions", peerConnectionOptions);
        
        sendMessage(ctx, config);
    }
    
    private void sendIdentifyMessage(ChannelHandlerContext ctx) {
        ObjectNode identify = objectMapper.createObjectNode();
        identify.put("type", "identify");
        sendMessage(ctx, identify);
    }
    
    public boolean isStreaming() {
        return streaming;
    }
    
    public int getMaxSubscribers() {
        return maxSubscribers;
    }
    
    public void setMaxSubscribers(int maxSubscribers) {
        this.maxSubscribers = maxSubscribers;
    }
}