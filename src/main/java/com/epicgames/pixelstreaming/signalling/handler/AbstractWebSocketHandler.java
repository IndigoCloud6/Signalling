// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import com.epicgames.pixelstreaming.signalling.message.BaseMessage;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.service.IConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract WebSocket frame handler for signalling connections.
 * Handles common WebSocket frame processing and delegates to connection-specific handlers.
 */
public abstract class AbstractWebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWebSocketHandler.class);

    protected final MessageHelper messageHelper;
    protected IConnection connection;

    public AbstractWebSocketHandler(MessageHelper messageHelper) {
        this.messageHelper = messageHelper;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            handleTextFrame(ctx, (TextWebSocketFrame) frame);
        } else {
            logger.warn("Received unsupported WebSocket frame type: {}", frame.getClass().getSimpleName());
        }
    }

    /**
     * Handle text WebSocket frames.
     */
    private void handleTextFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();
        logger.debug("Received WebSocket message: {}", text);

        try {
            BaseMessage message = messageHelper.parseMessage(text);
            if (message != null && connection != null) {
                connection.handleMessage(message);
            } else {
                logger.warn("Failed to parse message or connection not established: {}", text);
            }
        } catch (Exception e) {
            logger.error("Error processing WebSocket message: {}", text, e);
            // Send error message back to client
            BaseMessage errorMessage = messageHelper.createErrorMessage("Error processing message: " + e.getMessage());
            if (errorMessage != null && connection != null) {
                connection.sendMessage(errorMessage);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("WebSocket connection established from {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
        
        // Create connection instance
        this.connection = createConnection(ctx);
        onConnectionEstablished(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("WebSocket connection closed from {}", ctx.channel().remoteAddress());
        if (connection != null) {
            onConnectionClosed(ctx);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("WebSocket handler exception for {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    /**
     * Create a connection instance for this handler.
     * Must be implemented by subclasses.
     */
    protected abstract IConnection createConnection(ChannelHandlerContext ctx);

    /**
     * Called when the connection is established.
     * Subclasses can override for additional initialization.
     */
    protected void onConnectionEstablished(ChannelHandlerContext ctx) {
        // Default implementation does nothing
    }

    /**
     * Called when the connection is closed.
     * Subclasses can override for additional cleanup.
     */
    protected void onConnectionClosed(ChannelHandlerContext ctx) {
        // Default implementation does nothing
    }
}