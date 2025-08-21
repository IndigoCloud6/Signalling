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
 * Connection handler for SFU (Selective Forwarding Unit) connections.
 * An SFU acts as both a streamer and a player, forwarding streams between multiple participants.
 */
@Component
public class SfuConnectionHandler extends AbstractConnectionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SfuConnectionHandler.class);
    
    // SFU state as a player (subscribing to other streamers)
    private ConnectionHandler subscribedStreamer;
    
    // SFU state as a streamer (other players subscribing to it)
    private boolean streaming = false;
    private final Set<String> subscribers = ConcurrentHashMap.newKeySet();
    private int maxSubscribers = 0; // 0 means unlimited
    
    @Override
    public ConnectionAttributes.ConnectionType getConnectionType() {
        return ConnectionAttributes.ConnectionType.SFU;
    }
    
    @Override
    protected void registerMessageHandlers() {
        // Player-like messages (when SFU subscribes to streamers)
        registerHandler("subscribe", this::handleSubscribe);
        registerHandler("unsubscribe", this::handleUnsubscribe);
        registerHandler("listStreamers", this::handleListStreamers);
        
        // Streamer-like messages (when SFU acts as a streamer)
        registerHandler("endpointId", this::handleEndpointId);
        registerHandler("streamerDataChannels", this::handleStreamerDataChannels);
        registerHandler("startStreaming", this::handleStartStreaming);
        registerHandler("stopStreaming", this::handleStopStreaming);
        
        // Common messages
        registerHandler("ping", this::handlePing);
        
        // WebRTC signalling - SFU needs to route these appropriately
        registerHandler("offer", this::handleOffer);
        registerHandler("answer", this::handleAnswer);
        registerHandler("iceCandidate", this::forwardToStreamer);
        registerHandler("peerDataChannels", this::forwardToPlayer);
    }
    
    @Override
    protected void onConnectionEstablished() {
        logger.info("SFU connection {} established from {}", 
            connectionId, session.getRemoteAddress());
        
        // TODO: Set max subscribers from configuration
        this.maxSubscribers = 0; // Unlimited for now
    }
    
    @Override
    protected void onDisconnection(CloseStatus closeStatus) {
        // Cleanup as both player and streamer
        if (subscribedStreamer != null) {
            unsubscribeFromStreamer();
        }
        
        // Notify subscribers that SFU is disconnecting
        if (!subscribers.isEmpty()) {
            StreamerDisconnectedMessage disconnectMsg = new StreamerDisconnectedMessage();
            for (String playerId : subscribers) {
                ConnectionHandler player = registry.getPlayer(playerId);
                if (player != null) {
                    player.sendMessage(disconnectMsg);
                }
            }
        }
        
        streaming = false;
        subscribers.clear();
        
        logger.info("SFU {} disconnected", connectionId);
    }
    
    // Player-like behavior methods
    
    private void handleSubscribe(BaseMessage message) {
        if (!(message instanceof SubscribeMessage)) {
            logger.warn("Expected SubscribeMessage but got {}", message.getClass().getSimpleName());
            return;
        }
        
        SubscribeMessage subscribeMsg = (SubscribeMessage) message;
        String streamerId = subscribeMsg.getStreamerId();
        
        logger.info("SFU {} requesting to subscribe to streamer {}", connectionId, streamerId);
        
        ConnectionHandler streamer = registry.getStreamer(streamerId);
        if (streamer == null) {
            logger.error("Streamer {} not found for SFU {}", streamerId, connectionId);
            return;
        }
        
        // Unsubscribe from current streamer if any
        if (subscribedStreamer != null) {
            unsubscribeFromStreamer();
        }
        
        // Subscribe to new streamer
        subscribedStreamer = streamer;
        
        // Notify streamer of new SFU connection
        PlayerConnectedMessage playerConnected = new PlayerConnectedMessage(connectionId, true, true);
        streamer.sendMessage(playerConnected);
        
        logger.info("SFU {} successfully subscribed to streamer {}", connectionId, streamerId);
    }
    
    private void handleUnsubscribe(BaseMessage message) {
        if (subscribedStreamer != null) {
            unsubscribeFromStreamer();
            logger.info("SFU {} unsubscribed from streamer", connectionId);
        }
    }
    
    private void handleListStreamers(BaseMessage message) {
        String[] streamerIds = registry.getStreamingStreamerIds().toArray(new String[0]);
        StreamerListMessage listMsg = new StreamerListMessage(streamerIds);
        sendMessage(listMsg);
        
        logger.debug("Sent streamer list to SFU {}: {} streamers", connectionId, streamerIds.length);
    }
    
    // Streamer-like behavior methods
    
    private void handleEndpointId(BaseMessage message) {
        streaming = true;
        logger.info("SFU {} is now streaming", connectionId);
    }
    
    private void handleStreamerDataChannels(BaseMessage message) {
        if (!(message instanceof StreamerDataChannelsMessage)) {
            return;
        }
        
        StreamerDataChannelsMessage dataChannelsMsg = (StreamerDataChannelsMessage) message;
        dataChannelsMsg.setSfuId(connectionId);
        
        if (subscribedStreamer != null) {
            subscribedStreamer.sendMessage(dataChannelsMsg);
            logger.debug("Forwarded streamer data channels message from SFU {} to streamer", connectionId);
        }
    }
    
    private void handleStartStreaming(BaseMessage message) {
        streaming = true;
        logger.info("SFU {} started streaming", connectionId);
    }
    
    private void handleStopStreaming(BaseMessage message) {
        streaming = false;
        logger.info("SFU {} stopped streaming", connectionId);
    }
    
    // WebRTC routing methods
    
    private void handleOffer(BaseMessage message) {
        // SFU forwards offers to players
        forwardToPlayer(message);
    }
    
    private void handleAnswer(BaseMessage message) {
        // SFU forwards answers to streamers
        forwardToStreamer(message);
    }
    
    // Common methods
    
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
            logger.warn("SFU {} tried to send message to streamer but not subscribed", connectionId);
            return;
        }
        
        message.setPlayerId(connectionId);
        subscribedStreamer.sendMessage(message);
        logger.debug("Forwarded message type '{}' from SFU {} to streamer", 
            message.getType(), connectionId);
    }
    
    private void forwardToPlayer(BaseMessage message) {
        String playerId = message.getPlayerId();
        if (playerId == null) {
            logger.warn("SFU {} tried to send message to player without playerId", connectionId);
            return;
        }
        
        ConnectionHandler player = registry.getPlayer(playerId);
        if (player != null) {
            message.setPlayerId(null);
            player.sendMessage(message);
            logger.debug("Forwarded message type '{}' from SFU {} to player {}", 
                message.getType(), connectionId, playerId);
        } else {
            logger.warn("SFU {} tried to send message to non-existent player {}", 
                connectionId, playerId);
        }
    }
    
    private void unsubscribeFromStreamer() {
        if (subscribedStreamer != null) {
            PlayerDisconnectedMessage playerDisconnected = new PlayerDisconnectedMessage(connectionId);
            subscribedStreamer.sendMessage(playerDisconnected);
            subscribedStreamer = null;
        }
    }
    
    // Public methods for SFU state management
    
    /**
     * Adds a player subscriber to this SFU.
     */
    public boolean addSubscriber(String playerId) {
        if (maxSubscribers > 0 && subscribers.size() >= maxSubscribers) {
            logger.warn("SFU {} reached max subscribers limit: {}", connectionId, maxSubscribers);
            return false;
        }
        
        boolean added = subscribers.add(playerId);
        if (added) {
            logger.info("Player {} subscribed to SFU {} ({} total subscribers)", 
                playerId, connectionId, subscribers.size());
        }
        return added;
    }
    
    /**
     * Removes a player subscriber from this SFU.
     */
    public boolean removeSubscriber(String playerId) {
        boolean removed = subscribers.remove(playerId);
        if (removed) {
            logger.info("Player {} unsubscribed from SFU {} ({} remaining subscribers)", 
                playerId, connectionId, subscribers.size());
        }
        return removed;
    }
    
    /**
     * Checks if the SFU is currently streaming.
     */
    public boolean isStreaming() {
        return streaming;
    }
    
    /**
     * Gets the currently subscribed streamer.
     */
    public ConnectionHandler getSubscribedStreamer() {
        return subscribedStreamer;
    }
    
    /**
     * Gets the current subscriber count.
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }
}