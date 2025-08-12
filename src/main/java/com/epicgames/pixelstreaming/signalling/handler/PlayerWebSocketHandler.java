// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.handler;

import io.netty.channel.ChannelHandlerContext;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.service.IConnection;
import com.epicgames.pixelstreaming.signalling.service.PlayerConnection;
import com.epicgames.pixelstreaming.signalling.service.ConnectionManager;

/**
 * WebSocket handler for player connections.
 */
public class PlayerWebSocketHandler extends AbstractWebSocketHandler {

    private final ConnectionManager connectionManager;

    public PlayerWebSocketHandler(MessageHelper messageHelper, ConnectionManager connectionManager) {
        super(messageHelper);
        this.connectionManager = connectionManager;
    }

    @Override
    protected IConnection createConnection(ChannelHandlerContext ctx) {
        PlayerConnection playerConnection = new PlayerConnection(
            ctx.channel(), 
            messageHelper, 
            connectionManager
        );
        connectionManager.addPlayerConnection(playerConnection);
        return playerConnection;
    }
}