package com.signalling.service.connection;

import com.signalling.message.*;
import com.signalling.model.ConnectionAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connection handler for Streamer connections.
 * Handles streamer-specific signalling messages and routing to players.
 */
@Component
public class StreamerConnectionHandler extends AbstractConnectionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamerConnectionHandler.class);
    
    private boolean streaming = false;
    private final Set<String> subscribers = ConcurrentHashMap.newKeySet();
    private int maxSubscribers = 0; // 0 means unlimited
    
    @Override
    public ConnectionAttributes.ConnectionType getConnectionType() {
        return ConnectionAttributes.ConnectionType.STREAMER;
    }
    
    @Override
    protected void registerMessageHandlers() {
        registerHandler("endpointId", this::handleEndpointId);
        registerHandler("disconnectPlayer", this::handleDisconnectPlayer);
        registerHandler("layerPreference", this::handleLayerPreference);
        registerHandler("ping", this::handlePing);
        
        // WebRTC signalling messages that need to be forwarded to players
        registerHandler("offer", this::forwardToPlayer);
        registerHandler("answer", this::forwardToPlayer);
        registerHandler("iceCandidate", this::forwardToPlayer);
    }
    
    @Override
    protected void onConnectionEstablished() {
        logger.info("Streamer connection {} established from {}", 
            connectionId, session.getRemoteAddress());
        
        // TODO: Set max subscribers from configuration
        this.maxSubscribers = 0; // Unlimited for now
    }
    
    @Override
    protected void onDisconnection(CloseStatus closeStatus) {
        streaming = false;
        
        // Notify all subscribed players that streamer disconnected
        StreamerDisconnectedMessage disconnectMsg = new StreamerDisconnectedMessage();
        for (String playerId : subscribers) {
            ConnectionHandler player = registry.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(disconnectMsg);
            }
        }
        
        subscribers.clear();
        logger.info("Streamer {} disconnected, notified {} subscribers", 
            connectionId, subscribers.size());
    }
    
    private void handleEndpointId(BaseMessage message) {
        if (!(message instanceof EndpointIdMessage)) {
            logger.warn("Expected EndpointIdMessage but got {}", message.getClass().getSimpleName());
            return;
        }
        
        // Streamer is ready to stream when it sends endpoint ID
        streaming = true;
        
        EndpointIdMessage endpointMsg = (EndpointIdMessage) message;
        logger.info("Streamer {} is now streaming with endpoint ID: {}", 
            connectionId, endpointMsg.getId());
    }
    
    private void handleDisconnectPlayer(BaseMessage message) {
        if (!(message instanceof DisconnectPlayerMessage)) {
            logger.warn("Expected DisconnectPlayerMessage but got {}", message.getClass().getSimpleName());
            return;
        }
        
        DisconnectPlayerMessage disconnectMsg = (DisconnectPlayerMessage) message;
        String playerId = disconnectMsg.getPlayerId();
        
        if (playerId != null) {
            ConnectionHandler player = registry.getPlayer(playerId);
            if (player != null) {
                try {
                    player.getSession().close(CloseStatus.NORMAL.withReason(disconnectMsg.getReason()));
                    subscribers.remove(playerId);
                    logger.info("Streamer {} disconnected player {} with reason: {}", 
                        connectionId, playerId, disconnectMsg.getReason());
                } catch (Exception e) {
                    logger.error("Failed to disconnect player {} from streamer {}: {}", 
                        playerId, connectionId, e.getMessage(), e);
                }
            }
        }
    }
    
    private void handleLayerPreference(BaseMessage message) {
        if (!(message instanceof LayerPreferenceMessage)) {
            return;
        }
        
        LayerPreferenceMessage layerMsg = (LayerPreferenceMessage) message;
        logger.debug("Streamer {} received layer preference: spatial={}, temporal={}", 
            connectionId, layerMsg.getSpatialLayer(), layerMsg.getTemporalLayer());
        
        // Layer preference is typically passed through as-is
        // Could implement additional logic here for SFU scenarios
    }
    
    private void handlePing(BaseMessage message) {
        if (!(message instanceof PingMessage)) {
            return;
        }
        
        PingMessage pingMsg = (PingMessage) message;
        PongMessage pongMsg = new PongMessage(pingMsg.getTime());
        sendMessage(pongMsg);
    }
    
    private void forwardToPlayer(BaseMessage message) {
        String playerId = message.getPlayerId();
        if (playerId == null) {
            logger.warn("Streamer {} tried to send message without playerId", connectionId);
            return;
        }
        
        ConnectionHandler player = registry.getPlayer(playerId);
        if (player != null) {
            // Remove playerId before forwarding to player
            message.setPlayerId(null);
            player.sendMessage(message);
            logger.debug("Forwarded message type '{}' from streamer {} to player {}", 
                message.getType(), connectionId, playerId);
        } else {
            logger.warn("Streamer {} tried to send message to non-existent player {}", 
                connectionId, playerId);
        }
    }
    
    /**
     * Adds a player subscriber.
     */
    public boolean addSubscriber(String playerId) {
        if (maxSubscribers > 0 && subscribers.size() >= maxSubscribers) {
            logger.warn("Streamer {} reached max subscribers limit: {}", connectionId, maxSubscribers);
            return false;
        }
        
        boolean added = subscribers.add(playerId);
        if (added) {
            logger.info("Player {} subscribed to streamer {} ({} total subscribers)", 
                playerId, connectionId, subscribers.size());
        }
        return added;
    }
    
    /**
     * Removes a player subscriber.
     */
    public boolean removeSubscriber(String playerId) {
        boolean removed = subscribers.remove(playerId);
        if (removed) {
            logger.info("Player {} unsubscribed from streamer {} ({} remaining subscribers)", 
                playerId, connectionId, subscribers.size());
        }
        return removed;
    }
    
    /**
     * Checks if the streamer is currently streaming.
     */
    public boolean isStreaming() {
        return streaming;
    }
    
    /**
     * Gets the current subscriber count.
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }
    
    /**
     * Gets the maximum number of allowed subscribers.
     */
    public int getMaxSubscribers() {
        return maxSubscribers;
    }
    
    /**
     * Sets the maximum number of allowed subscribers.
     */
    public void setMaxSubscribers(int maxSubscribers) {
        this.maxSubscribers = maxSubscribers;
    }
}