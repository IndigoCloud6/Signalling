// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.service;

import io.netty.channel.Channel;
import com.epicgames.pixelstreaming.signalling.message.BaseMessage;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.message.MessageTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an SFU (Selective Forwarding Unit) connection in the signalling server.
 * SFUs handle multi-participant streaming scenarios.
 */
public class SFUConnection extends AbstractConnection {

    private static final Logger logger = LoggerFactory.getLogger(SFUConnection.class);

    private String sfuId;
    private final ConnectionManager connectionManager;

    public SFUConnection(Channel channel, MessageHelper messageHelper, ConnectionManager connectionManager) {
        super(channel, ConnectionType.SFU, messageHelper);
        this.connectionManager = connectionManager;
        onConnected();
    }

    @Override
    public void handleMessage(BaseMessage message) {
        updateLastActivity();
        
        if (message == null || message.getType() == null) {
            logger.warn("Received null or invalid message from SFU {}", id);
            return;
        }

        logger.debug("SFU {} received message: {}", id, message.getType());

        switch (message.getType()) {
            case MessageTypes.IDENTIFY:
                handleIdentifyMessage(message);
                break;
            
            case MessageTypes.SFU_RECV_DATA_CHANNEL_READY:
                handleSfuRecvDataChannelReady(message);
                break;
            
            case MessageTypes.SFU_PEER_DATA_CHANNELS_READY:
                handleSfuPeerDataChannelsReady(message);
                break;
            
            case MessageTypes.LAYER_PREFERENCE:
                handleLayerPreference(message);
                break;
            
            case MessageTypes.PING:
                sendMessage(messageHelper.createPongMessage());
                break;
            
            case MessageTypes.DISCONNECT:
                handleDisconnect();
                break;
            
            default:
                logger.debug("Unhandled message type from SFU {}: {}", id, message.getType());
                break;
        }
    }

    /**
     * Handle SFU identification message.
     */
    private void handleIdentifyMessage(BaseMessage message) {
        if (message.getData() != null && message.getData().has("sfuId")) {
            this.sfuId = message.getData().get("sfuId").asText();
            logger.info("SFU {} identified with ID: {}", id, sfuId);
        } else {
            this.sfuId = "sfu_" + id.substring(0, 8);
            logger.info("SFU {} assigned default ID: {}", id, sfuId);
        }

        // Send configuration to the SFU
        sendConfigMessage();
    }

    /**
     * Send configuration message to the SFU.
     */
    private void sendConfigMessage() {
        BaseMessage configMessage = messageHelper.createConfigMessage(null);
        sendMessage(configMessage);
    }

    /**
     * Handle SFU receive data channel ready message.
     */
    private void handleSfuRecvDataChannelReady(BaseMessage message) {
        logger.debug("SFU {} receive data channel ready", id);
        // Implementation would handle SFU-specific data channel setup
    }

    /**
     * Handle SFU peer data channels ready message.
     */
    private void handleSfuPeerDataChannelsReady(BaseMessage message) {
        logger.debug("SFU {} peer data channels ready", id);
        // Implementation would handle SFU peer setup
    }

    /**
     * Handle layer preference message from SFU.
     */
    private void handleLayerPreference(BaseMessage message) {
        logger.debug("SFU {} layer preference: {}", id, message);
        // Implementation would handle SFU layer preference routing
    }

    /**
     * Handle SFU disconnect.
     */
    private void handleDisconnect() {
        logger.info("SFU {} requested disconnect", id);
        close();
    }

    @Override
    protected void onDisconnected() {
        super.onDisconnected();
        connectionManager.removeSfuConnection(this);
    }

    // Getters
    public String getSfuId() {
        return sfuId;
    }
}