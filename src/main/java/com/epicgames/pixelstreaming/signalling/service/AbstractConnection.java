// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.service;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import com.epicgames.pixelstreaming.signalling.message.BaseMessage;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for WebSocket connections.
 * Provides common functionality shared by all connection types.
 */
public abstract class AbstractConnection implements IConnection {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConnection.class);

    protected final String id;
    protected final Channel channel;
    protected final ConnectionType type;
    protected final MessageHelper messageHelper;
    protected final long connectedAt;
    protected final AtomicLong lastActivity;

    public AbstractConnection(Channel channel, ConnectionType type, MessageHelper messageHelper) {
        this.id = UUID.randomUUID().toString();
        this.channel = channel;
        this.type = type;
        this.messageHelper = messageHelper;
        this.connectedAt = System.currentTimeMillis();
        this.lastActivity = new AtomicLong(this.connectedAt);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public ConnectionType getType() {
        return type;
    }

    @Override
    public void sendMessage(BaseMessage message) {
        if (!isActive()) {
            logger.warn("Attempted to send message to inactive connection {}: {}", id, message);
            return;
        }

        try {
            String json = messageHelper.serializeMessage(message);
            if (json != null) {
                channel.writeAndFlush(new TextWebSocketFrame(json));
                logger.debug("Sent message to connection {}: {}", id, message.getType());
            } else {
                logger.error("Failed to serialize message for connection {}: {}", id, message);
            }
        } catch (Exception e) {
            logger.error("Error sending message to connection {}: {}", id, message, e);
        }
    }

    @Override
    public abstract void handleMessage(BaseMessage message);

    @Override
    public void close() {
        if (channel.isActive()) {
            logger.info("Closing connection {}", id);
            channel.close();
        }
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public long getConnectedAt() {
        return connectedAt;
    }

    @Override
    public void updateLastActivity() {
        lastActivity.set(System.currentTimeMillis());
    }

    @Override
    public long getLastActivity() {
        return lastActivity.get();
    }

    /**
     * Called when the connection is established.
     * Subclasses can override this to perform initialization.
     */
    protected void onConnected() {
        logger.info("Connection {} of type {} established", id, type);
    }

    /**
     * Called when the connection is closed.
     * Subclasses can override this to perform cleanup.
     */
    protected void onDisconnected() {
        logger.info("Connection {} of type {} closed", id, type);
    }

    @Override
    public String toString() {
        return String.format("%s{id='%s', type=%s, active=%s}", 
                getClass().getSimpleName(), id, type, isActive());
    }
}