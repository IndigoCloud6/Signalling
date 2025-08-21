package com.epicgames.pixelstreaming.signalling.connection;

import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelHandlerContext;

/**
 * Connection handler for SFU (Selective Forwarding Unit) connections.
 * SFU connections act as both streamers and players, allowing them to
 * receive streams from other streamers and forward them to players.
 */
public class SFUConnectionHandler extends AbstractConnectionHandler {
    
    private boolean streaming = false;
    private String subscribedStreamerId;
    private int maxSubscribers = 0;
    
    public SFUConnectionHandler(ConnectionAttributes attributes, String remoteAddress) {
        super(attributes, remoteAddress);
    }
    
    @Override
    public void onConnectionEstablished(ChannelHandlerContext ctx) {
        super.onConnectionEstablished(ctx);
        
        // Send initial configuration to SFU
        sendConfigMessage(ctx);
        
        // Request SFU to identify itself
        sendIdentifyMessage(ctx);
    }
    
    @Override
    protected void handleMessage(ChannelHandlerContext ctx, JsonNode message) {
        String messageType = message.path("type").asText();
        
        switch (messageType) {
            // Streamer-like behavior
            case "endpointId":
                handleEndpointId(ctx, message);
                break;
            case "startStreaming":
                handleStartStreaming(ctx, message);
                break;
            case "stopStreaming":
                handleStopStreaming(ctx, message);
                break;
            case "disconnectPlayer":
                handleDisconnectPlayer(ctx, message);
                break;
                
            // Player-like behavior  
            case "subscribe":
                handleSubscribe(ctx, message);
                break;
            case "unsubscribe":
                handleUnsubscribe(ctx, message);
                break;
            case "listStreamers":
                handleListStreamers(ctx, message);
                break;
            case "streamerDataChannels":
                handleStreamerDataChannels(ctx, message);
                break;
                
            // Common messages
            case "ping":
                handlePing(ctx, message);
                break;
            case "offer":
            case "answer":
            case "iceCandidate":
                handleMediaMessage(ctx, message);
                break;
                
            default:
                logger.warn("Unknown message type from SFU {}: {}", getConnectionId(), messageType);
        }
    }
    
    // Streamer-like methods
    private void handleEndpointId(ChannelHandlerContext ctx, JsonNode message) {
        String sfuId = message.path("id").asText();
        logger.info("SFU {} identified as: {}", getConnectionId(), sfuId);
        
        // Send confirmation back to SFU
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "endpointIdConfirm");
        response.put("committedId", sfuId);
        sendMessage(ctx, response);
    }
    
    private void handleStartStreaming(ChannelHandlerContext ctx, JsonNode message) {
        logger.info("SFU {} started streaming", getConnectionId());
        streaming = true;
    }
    
    private void handleStopStreaming(ChannelHandlerContext ctx, JsonNode message) {
        logger.info("SFU {} stopped streaming", getConnectionId());
        streaming = false;
    }
    
    private void handleDisconnectPlayer(ChannelHandlerContext ctx, JsonNode message) {
        String playerId = message.path("playerId").asText();
        String reason = message.path("reason").asText("No reason provided");
        
        logger.info("SFU {} requested disconnect of player {}: {}", 
                   getConnectionId(), playerId, reason);
        
        // TODO: Implement player disconnection through connection registry
    }
    
    // Player-like methods
    private void handleSubscribe(ChannelHandlerContext ctx, JsonNode message) {
        String streamerId = message.path("streamerId").asText();
        logger.info("SFU {} subscribing to streamer: {}", getConnectionId(), streamerId);
        
        // Unsubscribe from current streamer if any
        if (subscribedStreamerId != null) {
            handleUnsubscribe(ctx, null);
        }
        
        // TODO: Validate streamer exists and is available through connection registry
        subscribedStreamerId = streamerId;
        
        // Send subscription confirmation
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "subscribed");
        response.put("streamerId", streamerId);
        sendMessage(ctx, response);
    }
    
    private void handleUnsubscribe(ChannelHandlerContext ctx, JsonNode message) {
        if (subscribedStreamerId != null) {
            logger.info("SFU {} unsubscribing from streamer: {}", getConnectionId(), subscribedStreamerId);
            
            // TODO: Notify streamer through connection registry
            subscribedStreamerId = null;
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("type", "unsubscribed");
            sendMessage(ctx, response);
        }
    }
    
    private void handleListStreamers(ChannelHandlerContext ctx, JsonNode message) {
        logger.debug("SFU {} requesting streamer list", getConnectionId());
        
        // TODO: Get available streamers from connection registry
        ArrayNode streamerIds = objectMapper.createArrayNode();
        // For now, return empty list
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "streamerList");
        response.set("ids", streamerIds);
        sendMessage(ctx, response);
    }
    
    private void handleStreamerDataChannels(ChannelHandlerContext ctx, JsonNode message) {
        // Add SFU ID and forward to streamer
        if (message instanceof ObjectNode) {
            ((ObjectNode) message).put("sfuId", getConnectionId());
        }
        
        if (subscribedStreamerId != null) {
            logger.debug("Forwarding data channel message from SFU {} to streamer {}", 
                        getConnectionId(), subscribedStreamerId);
            // TODO: Implement message forwarding through message router
        }
    }
    
    // Common methods
    private void handlePing(ChannelHandlerContext ctx, JsonNode message) {
        ObjectNode pong = objectMapper.createObjectNode();
        pong.put("type", "pong");
        if (message.has("time")) {
            pong.put("time", message.get("time").asLong());
        }
        sendMessage(ctx, pong);
    }
    
    private void handleMediaMessage(ChannelHandlerContext ctx, JsonNode message) {
        // SFU can both receive and send media messages
        String playerId = message.path("playerId").asText();
        
        if (!playerId.isEmpty()) {
            // Forward to specific player
            logger.debug("Forwarding media message from SFU {} to player {}", 
                        getConnectionId(), playerId);
            // TODO: Implement message forwarding through message router
        } else if (subscribedStreamerId != null) {
            // Forward to subscribed streamer
            logger.debug("Forwarding media message from SFU {} to streamer {}", 
                        getConnectionId(), subscribedStreamerId);
            
            // Add SFU ID to the message
            if (message instanceof ObjectNode) {
                ((ObjectNode) message).put("playerId", getConnectionId());
            }
            
            // TODO: Implement message forwarding through message router
        }
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
    
    public String getSubscribedStreamerId() {
        return subscribedStreamerId;
    }
    
    public int getMaxSubscribers() {
        return maxSubscribers;
    }
    
    public void setMaxSubscribers(int maxSubscribers) {
        this.maxSubscribers = maxSubscribers;
    }
}