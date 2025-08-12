// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.service;

import io.netty.channel.Channel;
import com.epicgames.pixelstreaming.signalling.message.BaseMessage;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.message.MessageTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a player connection in the signalling server.
 * Players connect to consume Pixel Streaming content from streamers.
 */
public class PlayerConnection extends AbstractConnection {

    private static final Logger logger = LoggerFactory.getLogger(PlayerConnection.class);

    private String playerId;
    private StreamerConnection subscribedStreamer;
    private final ConnectionManager connectionManager;

    public PlayerConnection(Channel channel, MessageHelper messageHelper, ConnectionManager connectionManager) {
        super(channel, ConnectionType.PLAYER, messageHelper);
        this.connectionManager = connectionManager;
        onConnected();
    }

    @Override
    public void handleMessage(BaseMessage message) {
        updateLastActivity();
        
        if (message == null || message.getType() == null) {
            logger.warn("Received null or invalid message from player {}", id);
            return;
        }

        logger.debug("Player {} received message: {}", id, message.getType());

        switch (message.getType()) {
            case MessageTypes.IDENTIFY:
                handleIdentifyMessage(message);
                break;
            
            case MessageTypes.OFFER:
            case MessageTypes.ANSWER:
            case MessageTypes.ICE_CANDIDATE:
            case MessageTypes.ICE_CANDIDATE_ERROR:
                forwardToStreamer(message);
                break;
            
            case MessageTypes.DATA_CHANNEL_REQUEST:
                handleDataChannelRequest(message);
                break;
            
            case MessageTypes.PING:
                sendMessage(messageHelper.createPongMessage());
                break;
            
            case MessageTypes.DISCONNECT:
                handleDisconnect();
                break;
            
            default:
                logger.debug("Unhandled message type from player {}: {}", id, message.getType());
                break;
        }
    }

    /**
     * Handle player identification message.
     */
    private void handleIdentifyMessage(BaseMessage message) {
        if (message.getData() != null && message.getData().has("playerId")) {
            this.playerId = message.getData().get("playerId").asText();
            logger.info("Player {} identified with ID: {}", id, playerId);
        }

        // Send configuration to the player
        sendConfigMessage();
        
        // Try to subscribe to an available streamer
        subscribeToStreamer();
    }

    /**
     * Send configuration message to the player.
     */
    private void sendConfigMessage() {
        // Get peer options from configuration or create default
        BaseMessage configMessage = messageHelper.createConfigMessage(null);
        sendMessage(configMessage);
    }

    /**
     * Subscribe this player to an available streamer.
     */
    private void subscribeToStreamer() {
        StreamerConnection availableStreamer = connectionManager.getAvailableStreamer();
        if (availableStreamer != null) {
            subscribeToStreamer(availableStreamer);
        } else {
            logger.info("No available streamer for player {}", id);
        }
    }

    /**
     * Subscribe this player to a specific streamer.
     */
    public void subscribeToStreamer(StreamerConnection streamer) {
        if (subscribedStreamer != null) {
            unsubscribeFromStreamer();
        }

        this.subscribedStreamer = streamer;
        streamer.addSubscriber(this);
        
        logger.info("Player {} subscribed to streamer {}", id, streamer.getId());
        
        // Notify about player count change
        connectionManager.notifyPlayerCountChanged();
    }

    /**
     * Unsubscribe this player from their current streamer.
     */
    public void unsubscribeFromStreamer() {
        if (subscribedStreamer != null) {
            subscribedStreamer.removeSubscriber(this);
            logger.info("Player {} unsubscribed from streamer {}", id, subscribedStreamer.getId());
            subscribedStreamer = null;
            connectionManager.notifyPlayerCountChanged();
        }
    }

    /**
     * Forward a message to the subscribed streamer.
     */
    private void forwardToStreamer(BaseMessage message) {
        if (subscribedStreamer != null && subscribedStreamer.isActive()) {
            // Add player ID to the message for routing
            if (message.getId() == null) {
                message.setId(id);
            }
            subscribedStreamer.sendMessage(message);
            logger.debug("Forwarded message from player {} to streamer {}: {}", 
                        id, subscribedStreamer.getId(), message.getType());
        } else {
            logger.warn("Player {} attempted to send message without active streamer: {}", 
                       id, message.getType());
            BaseMessage errorMessage = messageHelper.createErrorMessage("No active streamer available");
            sendMessage(errorMessage);
        }
    }

    /**
     * Handle data channel request from player.
     */
    private void handleDataChannelRequest(BaseMessage message) {
        // Forward to streamer if available
        forwardToStreamer(message);
    }

    /**
     * Handle player disconnect.
     */
    private void handleDisconnect() {
        logger.info("Player {} requested disconnect", id);
        close();
    }

    @Override
    protected void onDisconnected() {
        super.onDisconnected();
        unsubscribeFromStreamer();
        connectionManager.removePlayerConnection(this);
    }

    // Getters
    public String getPlayerId() {
        return playerId;
    }

    public StreamerConnection getSubscribedStreamer() {
        return subscribedStreamer;
    }

    public boolean hasSubscribedStreamer() {
        return subscribedStreamer != null && subscribedStreamer.isActive();
    }
}