// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.handler;

import io.netty.channel.ChannelHandlerContext;
import com.epicgames.pixelstreaming.signalling.config.SignallingConfig;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.service.IConnection;
import com.epicgames.pixelstreaming.signalling.service.StreamerConnection;
import com.epicgames.pixelstreaming.signalling.service.ConnectionManager;

/**
 * WebSocket handler for streamer connections.
 */
public class StreamerWebSocketHandler extends AbstractWebSocketHandler {

    private final ConnectionManager connectionManager;
    private final SignallingConfig config;

    public StreamerWebSocketHandler(MessageHelper messageHelper, ConnectionManager connectionManager, 
                                  SignallingConfig config) {
        super(messageHelper);
        this.connectionManager = connectionManager;
        this.config = config;
    }

    @Override
    protected IConnection createConnection(ChannelHandlerContext ctx) {
        StreamerConnection streamerConnection = new StreamerConnection(
            ctx.channel(), 
            messageHelper, 
            connectionManager,
            config.getServer().getMaxSubscribers()
        );
        connectionManager.addStreamerConnection(streamerConnection);
        return streamerConnection;
    }
}