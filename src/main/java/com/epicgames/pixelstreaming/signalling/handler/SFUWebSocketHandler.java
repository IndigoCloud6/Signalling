// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.handler;

import io.netty.channel.ChannelHandlerContext;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.service.IConnection;
import com.epicgames.pixelstreaming.signalling.service.SFUConnection;
import com.epicgames.pixelstreaming.signalling.service.ConnectionManager;

/**
 * WebSocket handler for SFU connections.
 */
public class SFUWebSocketHandler extends AbstractWebSocketHandler {

    private final ConnectionManager connectionManager;

    public SFUWebSocketHandler(MessageHelper messageHelper, ConnectionManager connectionManager) {
        super(messageHelper);
        this.connectionManager = connectionManager;
    }

    @Override
    protected IConnection createConnection(ChannelHandlerContext ctx) {
        SFUConnection sfuConnection = new SFUConnection(
            ctx.channel(), 
            messageHelper, 
            connectionManager
        );
        connectionManager.addSfuConnection(sfuConnection);
        return sfuConnection;
    }
}