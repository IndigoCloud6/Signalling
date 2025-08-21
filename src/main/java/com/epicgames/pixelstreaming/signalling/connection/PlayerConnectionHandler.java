package com.epicgames.pixelstreaming.signalling.connection;

import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelHandlerContext;

/**
 * Connection handler for player connections.
 * Players consume media streams from streamers.
 */
public class PlayerConnectionHandler extends AbstractConnectionHandler {
    
    private String subscribedStreamerId;
    
    public PlayerConnectionHandler(ConnectionAttributes attributes, String remoteAddress) {
        super(attributes, remoteAddress);
    }
    
    @Override
    public void onConnectionEstablished(ChannelHandlerContext ctx) {
        super.onConnectionEstablished(ctx);
        
        // Send initial configuration to player
        sendConfigMessage(ctx);
    }
    
    @Override
    protected void handleMessage(ChannelHandlerContext ctx, JsonNode message) {
        String messageType = message.path("type").asText();
        
        switch (messageType) {
            case "subscribe":
                handleSubscribe(ctx, message);
                break;
            case "unsubscribe":
                handleUnsubscribe(ctx, message);
                break;
            case "listStreamers":
                handleListStreamers(ctx, message);
                break;
            case "ping":
                handlePing(ctx, message);
                break;
            case "offer":
            case "answer":
            case "iceCandidate":
            case "dataChannelRequest":
            case "peerDataChannelsReady":
                handleMediaMessage(ctx, message);
                break;
            default:
                logger.warn("Unknown message type from player {}: {}", getConnectionId(), messageType);
        }
    }
    
    private void handleSubscribe(ChannelHandlerContext ctx, JsonNode message) {
        String streamerId = message.path("streamerId").asText();
        logger.info("Player {} subscribing to streamer: {}", getConnectionId(), streamerId);
        
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
            logger.info("Player {} unsubscribing from streamer: {}", getConnectionId(), subscribedStreamerId);
            
            // TODO: Notify streamer through connection registry
            subscribedStreamerId = null;
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("type", "unsubscribed");
            sendMessage(ctx, response);
        }
    }
    
    private void handleListStreamers(ChannelHandlerContext ctx, JsonNode message) {
        logger.debug("Player {} requesting streamer list", getConnectionId());
        
        // TODO: Get available streamers from connection registry
        ArrayNode streamerIds = objectMapper.createArrayNode();
        // For now, return empty list
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "streamerList");
        response.set("ids", streamerIds);
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
        // Forward media messages to the subscribed streamer
        if (subscribedStreamerId != null) {
            logger.debug("Forwarding media message from player {} to streamer {}", 
                        getConnectionId(), subscribedStreamerId);
            
            // Add player ID to the message for the streamer
            if (message instanceof ObjectNode) {
                ((ObjectNode) message).put("playerId", getConnectionId());
            }
            
            // TODO: Implement message forwarding through message router
        } else {
            logger.warn("Player {} sent media message but is not subscribed to any streamer", 
                       getConnectionId());
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
    
    public String getSubscribedStreamerId() {
        return subscribedStreamerId;
    }
}