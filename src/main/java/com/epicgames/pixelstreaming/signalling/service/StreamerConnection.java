// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.service;

import io.netty.channel.Channel;
import com.epicgames.pixelstreaming.signalling.message.BaseMessage;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.message.MessageTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a streamer connection in the signalling server.
 * Streamers provide Pixel Streaming content to connected players.
 */
public class StreamerConnection extends AbstractConnection {

    private static final Logger logger = LoggerFactory.getLogger(StreamerConnection.class);

    private String streamerId;
    private final Set<PlayerConnection> subscribers;
    private final ConnectionManager connectionManager;
    private final int maxSubscribers;

    public StreamerConnection(Channel channel, MessageHelper messageHelper, 
                            ConnectionManager connectionManager, int maxSubscribers) {
        super(channel, ConnectionType.STREAMER, messageHelper);
        this.connectionManager = connectionManager;
        this.maxSubscribers = maxSubscribers;
        this.subscribers = ConcurrentHashMap.newKeySet();
        onConnected();
    }

    @Override
    public void handleMessage(BaseMessage message) {
        updateLastActivity();
        
        if (message == null || message.getType() == null) {
            logger.warn("Received null or invalid message from streamer {}", id);
            return;
        }

        logger.debug("Streamer {} received message: {}", id, message.getType());

        switch (message.getType()) {
            case MessageTypes.IDENTIFY:
                handleIdentifyMessage(message);
                break;
            
            case MessageTypes.OFFER:
            case MessageTypes.ANSWER:
            case MessageTypes.ICE_CANDIDATE:
            case MessageTypes.ICE_CANDIDATE_ERROR:
                forwardToPlayer(message);
                break;
            
            case MessageTypes.STREAMER_DATA_CHANNELS:
                handleStreamerDataChannels(message);
                break;
            
            case MessageTypes.PING:
                sendMessage(messageHelper.createPongMessage());
                break;
            
            case MessageTypes.DISCONNECT:
                handleDisconnect();
                break;
            
            default:
                logger.debug("Unhandled message type from streamer {}: {}", id, message.getType());
                break;
        }
    }

    /**
     * Handle streamer identification message.
     */
    private void handleIdentifyMessage(BaseMessage message) {
        if (message.getData() != null && message.getData().has("streamerId")) {
            String newStreamerId = message.getData().get("streamerId").asText();
            if (!newStreamerId.equals(this.streamerId)) {
                String oldStreamerId = this.streamerId;
                this.streamerId = newStreamerId;
                logger.info("Streamer {} changed ID from {} to {}", id, oldStreamerId, streamerId);
                
                // Notify subscribers about ID change
                notifySubscribersStreamerIdChanged();
            }
        } else {
            // Generate default streamer ID if not provided
            this.streamerId = "streamer_" + id.substring(0, 8);
            logger.info("Streamer {} assigned default ID: {}", id, streamerId);
        }

        // Send configuration to the streamer
        sendConfigMessage();
    }

    /**
     * Send configuration message to the streamer.
     */
    private void sendConfigMessage() {
        BaseMessage configMessage = messageHelper.createConfigMessage(null);
        sendMessage(configMessage);
    }

    /**
     * Handle streamer data channels message.
     */
    private void handleStreamerDataChannels(BaseMessage message) {
        logger.debug("Streamer {} provided data channels info", id);
        // Forward to all subscribers
        broadcastToSubscribers(message);
    }

    /**
     * Forward a message to a specific player.
     */
    private void forwardToPlayer(BaseMessage message) {
        String playerId = message.getId();
        if (playerId != null) {
            PlayerConnection targetPlayer = findPlayerById(playerId);
            if (targetPlayer != null && targetPlayer.isActive()) {
                targetPlayer.sendMessage(message);
                logger.debug("Forwarded message from streamer {} to player {}: {}", 
                           id, playerId, message.getType());
            } else {
                logger.warn("Streamer {} attempted to send message to unknown/inactive player {}: {}", 
                           id, playerId, message.getType());
            }
        } else {
            logger.warn("Streamer {} sent message without player ID: {}", id, message.getType());
        }
    }

    /**
     * Find a subscribed player by their connection ID.
     */
    private PlayerConnection findPlayerById(String playerId) {
        return subscribers.stream()
            .filter(player -> player.getId().equals(playerId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Broadcast a message to all subscribers.
     */
    private void broadcastToSubscribers(BaseMessage message) {
        if (subscribers.isEmpty()) {
            logger.debug("No subscribers to broadcast message to from streamer {}", id);
            return;
        }

        logger.debug("Broadcasting message from streamer {} to {} subscribers: {}", 
                    id, subscribers.size(), message.getType());
        
        subscribers.removeIf(player -> {
            if (player.isActive()) {
                player.sendMessage(message);
                return false;
            } else {
                logger.debug("Removing inactive subscriber {} from streamer {}", player.getId(), id);
                return true;
            }
        });
    }

    /**
     * Add a player subscriber to this streamer.
     */
    public boolean addSubscriber(PlayerConnection player) {
        if (subscribers.size() >= maxSubscribers) {
            logger.warn("Streamer {} has reached maximum subscribers ({}/{})", 
                       id, subscribers.size(), maxSubscribers);
            return false;
        }

        boolean added = subscribers.add(player);
        if (added) {
            logger.info("Added subscriber {} to streamer {} ({}/{})", 
                       player.getId(), id, subscribers.size(), maxSubscribers);
            
            // Send player count update
            sendPlayerCountMessage();
        }
        return added;
    }

    /**
     * Remove a player subscriber from this streamer.
     */
    public boolean removeSubscriber(PlayerConnection player) {
        boolean removed = subscribers.remove(player);
        if (removed) {
            logger.info("Removed subscriber {} from streamer {} ({}/{})", 
                       player.getId(), id, subscribers.size(), maxSubscribers);
            
            // Send player count update
            sendPlayerCountMessage();
        }
        return removed;
    }

    /**
     * Send player count message to the streamer.
     */
    private void sendPlayerCountMessage() {
        BaseMessage playerCountMessage = messageHelper.createPlayerCountMessage(subscribers.size());
        sendMessage(playerCountMessage);
    }

    /**
     * Notify subscribers about streamer ID change.
     */
    private void notifySubscribersStreamerIdChanged() {
        // Implementation would create appropriate message
        logger.debug("Notifying {} subscribers about streamer ID change", subscribers.size());
    }

    /**
     * Handle streamer disconnect.
     */
    private void handleDisconnect() {
        logger.info("Streamer {} requested disconnect", id);
        close();
    }

    @Override
    protected void onDisconnected() {
        super.onDisconnected();
        
        // Disconnect all subscribers
        subscribers.forEach(player -> {
            if (player.isActive()) {
                player.unsubscribeFromStreamer();
            }
        });
        subscribers.clear();
        
        connectionManager.removeStreamerConnection(this);
    }

    /**
     * Check if this streamer can accept more subscribers.
     */
    public boolean canAcceptSubscribers() {
        return subscribers.size() < maxSubscribers && isActive();
    }

    /**
     * Get the number of current subscribers.
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }

    /**
     * Get maximum number of subscribers.
     */
    public int getMaxSubscribers() {
        return maxSubscribers;
    }

    // Getters
    public String getStreamerId() {
        return streamerId;
    }

    public Set<PlayerConnection> getSubscribers() {
        return Set.copyOf(subscribers);
    }
}