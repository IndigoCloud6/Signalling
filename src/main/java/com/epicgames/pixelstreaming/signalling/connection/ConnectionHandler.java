package com.epicgames.pixelstreaming.signalling.connection;

import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * Base interface for handling WebSocket connections in the signalling server.
 * Implements the Strategy pattern for different connection types.
 */
public interface ConnectionHandler {
    
    /**
     * Gets the connection attributes for this handler.
     * 
     * @return The connection attributes
     */
    ConnectionAttributes getAttributes();
    
    /**
     * Gets the unique identifier for this connection.
     * 
     * @return The connection ID
     */
    String getConnectionId();
    
    /**
     * Gets the remote address of this connection.
     * 
     * @return The remote address as a string
     */
    String getRemoteAddress();
    
    /**
     * Called when the WebSocket connection is established.
     * 
     * @param ctx The Netty channel handler context
     */
    void onConnectionEstablished(ChannelHandlerContext ctx);
    
    /**
     * Called when a text message is received from the client.
     * 
     * @param ctx The Netty channel handler context  
     * @param message The received message
     */
    void onTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame message);
    
    /**
     * Called when the WebSocket connection is closed.
     * 
     * @param ctx The Netty channel handler context
     */
    void onConnectionClosed(ChannelHandlerContext ctx);
    
    /**
     * Called when an error occurs on the connection.
     * 
     * @param ctx The Netty channel handler context
     * @param cause The error cause
     */
    void onError(ChannelHandlerContext ctx, Throwable cause);
    
    /**
     * Sends a message to the client.
     * 
     * @param ctx The Netty channel handler context
     * @param message The message to send
     */
    void sendMessage(ChannelHandlerContext ctx, JsonNode message);
    
    /**
     * Sends a text message to the client.
     * 
     * @param ctx The Netty channel handler context
     * @param message The text message to send
     */
    void sendTextMessage(ChannelHandlerContext ctx, String message);
    
    /**
     * Checks if this connection is still active.
     * 
     * @return true if the connection is active, false otherwise
     */
    boolean isActive();
}