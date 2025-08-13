// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.service;

import io.netty.channel.Channel;
import com.epicgames.pixelstreaming.signalling.message.BaseMessage;

/**
 * Interface for managing WebSocket connections in the signalling server.
 * Provides common functionality for player, streamer, and SFU connections.
 */
public interface IConnection {

    /**
     * Get the unique identifier for this connection.
     *
     * @return The connection ID
     */
    String getId();

    /**
     * Get the Netty channel for this connection.
     *
     * @return The channel
     */
    Channel getChannel();

    /**
     * Get the connection type (player, streamer, sfu).
     *
     * @return The connection type
     */
    ConnectionType getType();

    /**
     * Send a message to this connection.
     *
     * @param message The message to send
     */
    void sendMessage(BaseMessage message);

    /**
     * Handle an incoming message from this connection.
     *
     * @param message The received message
     */
    void handleMessage(BaseMessage message);

    /**
     * Close this connection.
     */
    void close();

    /**
     * Check if this connection is active.
     *
     * @return true if the connection is active
     */
    boolean isActive();

    /**
     * Get the timestamp when this connection was established.
     *
     * @return The connection timestamp in milliseconds
     */
    long getConnectedAt();

    /**
     * Update the last activity timestamp for this connection.
     */
    void updateLastActivity();

    /**
     * Get the timestamp of the last activity on this connection.
     *
     * @return The last activity timestamp in milliseconds
     */
    long getLastActivity();

    /**
     * Enumeration of connection types.
     */
    enum ConnectionType {
        PLAYER,
        STREAMER,
        SFU
    }
}