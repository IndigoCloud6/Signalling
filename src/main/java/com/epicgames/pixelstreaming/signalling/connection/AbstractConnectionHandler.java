package com.epicgames.pixelstreaming.signalling.connection;

import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for WebSocket connection handlers.
 * Provides common functionality for all connection types.
 */
public abstract class AbstractConnectionHandler implements ConnectionHandler {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String connectionId;
    private final ConnectionAttributes attributes;
    private final String remoteAddress;
    private final AtomicBoolean active = new AtomicBoolean(false);
    
    protected AbstractConnectionHandler(ConnectionAttributes attributes, String remoteAddress) {
        this.connectionId = generateConnectionId();
        this.attributes = attributes;
        this.remoteAddress = remoteAddress;
    }
    
    @Override
    public ConnectionAttributes getAttributes() {
        return attributes;
    }
    
    @Override
    public String getConnectionId() {
        return connectionId;
    }
    
    @Override
    public String getRemoteAddress() {
        return remoteAddress;
    }
    
    @Override
    public void onConnectionEstablished(ChannelHandlerContext ctx) {
        active.set(true);
        logger.info("Connection established: {} {} from {}", 
                   getAttributes().getType(), getConnectionId(), getRemoteAddress());
    }
    
    @Override
    public void onTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame message) {
        try {
            String text = message.text();
            logger.debug("Received message from {}: {}", getConnectionId(), text);
            
            JsonNode jsonMessage = objectMapper.readTree(text);
            handleMessage(ctx, jsonMessage);
            
        } catch (Exception e) {
            logger.error("Error processing message from {}: {}", getConnectionId(), e.getMessage(), e);
            onError(ctx, e);
        }
    }
    
    @Override
    public void onConnectionClosed(ChannelHandlerContext ctx) {
        active.set(false);
        logger.info("Connection closed: {} {} from {}", 
                   getAttributes().getType(), getConnectionId(), getRemoteAddress());
    }
    
    @Override
    public void onError(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error in connection {} {}: {}", 
                    getAttributes().getType(), getConnectionId(), cause.getMessage(), cause);
    }
    
    @Override
    public void sendMessage(ChannelHandlerContext ctx, JsonNode message) {
        try {
            String text = objectMapper.writeValueAsString(message);
            sendTextMessage(ctx, text);
        } catch (Exception e) {
            logger.error("Error serializing message for {}: {}", getConnectionId(), e.getMessage(), e);
        }
    }
    
    @Override
    public void sendTextMessage(ChannelHandlerContext ctx, String message) {
        if (ctx != null && ctx.channel().isActive()) {
            logger.debug("Sending message to {}: {}", getConnectionId(), message);
            ctx.writeAndFlush(new TextWebSocketFrame(message));
        } else {
            logger.warn("Cannot send message to {}: channel is not active", getConnectionId());
        }
    }
    
    @Override
    public boolean isActive() {
        return active.get();
    }
    
    /**
     * Handles a parsed JSON message. Subclasses should implement this method
     * to handle specific message types.
     * 
     * @param ctx The Netty channel handler context
     * @param message The parsed JSON message
     */
    protected abstract void handleMessage(ChannelHandlerContext ctx, JsonNode message);
    
    /**
     * Generates a unique connection ID.
     * 
     * @return A unique connection ID
     */
    private String generateConnectionId() {
        return attributes.getType().getValue() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}