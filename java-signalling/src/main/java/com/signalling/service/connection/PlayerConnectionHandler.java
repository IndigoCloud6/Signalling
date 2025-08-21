package com.signalling.service.connection;

import com.signalling.message.*;
import com.signalling.model.ConnectionAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

/**
 * Connection handler for Player connections.
 * Handles player-specific signalling messages and routing to streamers.
 */
@Component
public class PlayerConnectionHandler extends AbstractConnectionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(PlayerConnectionHandler.class);
    
    private ConnectionHandler subscribedStreamer;
    
    @Override
    public ConnectionAttributes.ConnectionType getConnectionType() {
        return ConnectionAttributes.ConnectionType.PLAYER;
    }
    
    @Override
    protected void registerMessageHandlers() {
        registerHandler("subscribe", this::handleSubscribe);
        registerHandler("unsubscribe", this::handleUnsubscribe);
        registerHandler("listStreamers", this::handleListStreamers);
        registerHandler("ping", this::handlePing);
        
        // WebRTC signalling messages that need to be forwarded to streamer
        registerHandler("offer", this::forwardToStreamer);
        registerHandler("answer", this::forwardToStreamer);
        registerHandler("iceCandidate", this::forwardToStreamer);
        registerHandler("dataChannelRequest", this::forwardToStreamer);
        registerHandler("peerDataChannelsReady", this::forwardToStreamer);
        registerHandler("layerPreference", this::forwardToStreamer);
    }
    
    @Override
    protected void onConnectionEstablished() {
        logger.info("Player connection {} established from {}", 
            connectionId, session.getRemoteAddress());
    }
    
    @Override
    protected void onDisconnection(CloseStatus closeStatus) {
        if (subscribedStreamer != null) {
            unsubscribeFromStreamer();
        }
    }
    
    private void handleSubscribe(BaseMessage message) {
        if (!(message instanceof SubscribeMessage)) {
            logger.warn("Expected SubscribeMessage but got {}", message.getClass().getSimpleName());
            return;
        }
        
        SubscribeMessage subscribeMsg = (SubscribeMessage) message;
        String streamerId = subscribeMsg.getStreamerId();
        
        logger.info("Player {} requesting to subscribe to streamer {}", connectionId, streamerId);
        
        ConnectionHandler streamer = registry.getStreamer(streamerId);
        if (streamer == null) {
            logger.error("Streamer {} not found for player {}", streamerId, connectionId);
            SubscribeFailedMessage failMsg = new SubscribeFailedMessage("Streamer " + streamerId + " does not exist.");
            sendMessage(failMsg);
            return;
        }
        
        // Unsubscribe from current streamer if any
        if (subscribedStreamer != null) {
            unsubscribeFromStreamer();
        }
        
        // Subscribe to new streamer
        subscribedStreamer = streamer;
        
        // Notify streamer of new player
        PlayerConnectedMessage playerConnected = new PlayerConnectedMessage(connectionId, true, false);
        streamer.sendMessage(playerConnected);
        
        logger.info("Player {} successfully subscribed to streamer {}", connectionId, streamerId);
    }
    
    private void handleUnsubscribe(BaseMessage message) {
        if (subscribedStreamer != null) {
            unsubscribeFromStreamer();
            logger.info("Player {} unsubscribed from streamer", connectionId);
        }
    }
    
    private void handleListStreamers(BaseMessage message) {
        String[] streamerIds = registry.getStreamingStreamerIds().toArray(new String[0]);
        StreamerListMessage listMsg = new StreamerListMessage(streamerIds);
        sendMessage(listMsg);
        
        logger.debug("Sent streamer list to player {}: {} streamers", connectionId, streamerIds.length);
    }
    
    private void handlePing(BaseMessage message) {
        if (!(message instanceof PingMessage)) {
            return;
        }
        
        PingMessage pingMsg = (PingMessage) message;
        PongMessage pongMsg = new PongMessage(pingMsg.getTime());
        sendMessage(pongMsg);
    }
    
    private void forwardToStreamer(BaseMessage message) {
        if (subscribedStreamer == null) {
            logger.warn("Player {} tried to send message to streamer but not subscribed", connectionId);
            
            // Try to auto-subscribe to first available streamer
            String firstStreamerId = registry.getFirstStreamerId();
            if (firstStreamerId != null) {
                logger.info("Auto-subscribing player {} to first available streamer {}", 
                    connectionId, firstStreamerId);
                SubscribeMessage autoSub = new SubscribeMessage(firstStreamerId);
                handleSubscribe(autoSub);
            } else {
                logger.error("No streamers available for auto-subscription");
                return;
            }
        }
        
        if (subscribedStreamer != null) {
            // Set player ID for the message
            message.setPlayerId(connectionId);
            subscribedStreamer.sendMessage(message);
            logger.debug("Forwarded message type '{}' from player {} to streamer", 
                message.getType(), connectionId);
        }
    }
    
    private void unsubscribeFromStreamer() {
        if (subscribedStreamer != null) {
            PlayerDisconnectedMessage playerDisconnected = new PlayerDisconnectedMessage(connectionId);
            subscribedStreamer.sendMessage(playerDisconnected);
            subscribedStreamer = null;
        }
    }
    
    /**
     * Gets the currently subscribed streamer.
     */
    public ConnectionHandler getSubscribedStreamer() {
        return subscribedStreamer;
    }
}