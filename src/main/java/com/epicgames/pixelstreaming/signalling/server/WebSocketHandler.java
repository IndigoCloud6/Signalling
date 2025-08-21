package com.epicgames.pixelstreaming.signalling.server;

import com.epicgames.pixelstreaming.signalling.config.SignallingProperties;
import com.epicgames.pixelstreaming.signalling.connection.ConnectionHandler;
import com.epicgames.pixelstreaming.signalling.factory.ConnectionHandlerFactory;
import com.epicgames.pixelstreaming.signalling.model.ConnectionAttributes;
import com.epicgames.pixelstreaming.signalling.registry.ConnectionRegistry;
import com.epicgames.pixelstreaming.signalling.util.UrlParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty WebSocket handler for managing WebSocket connections.
 * This handler processes WebSocket handshakes and manages the connection lifecycle.
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final AttributeKey<ConnectionHandler> CONNECTION_HANDLER_KEY = 
            AttributeKey.valueOf("CONNECTION_HANDLER");
    
    private final ConnectionHandlerFactory connectionHandlerFactory;
    private final ConnectionRegistry connectionRegistry;
    private final SignallingProperties properties;
    
    private WebSocketServerHandshaker handshaker;
    
    public WebSocketHandler(ConnectionHandlerFactory connectionHandlerFactory,
                           ConnectionRegistry connectionRegistry,
                           SignallingProperties properties) {
        this.connectionHandlerFactory = connectionHandlerFactory;
        this.connectionRegistry = connectionRegistry;
        this.properties = properties;
    }
    
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ConnectionHandler handler = ctx.channel().attr(CONNECTION_HANDLER_KEY).get();
        if (handler != null) {
            handler.onConnectionClosed(ctx);
            connectionRegistry.unregisterConnection(handler);
        }
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("WebSocket error", cause);
        
        ConnectionHandler handler = ctx.channel().attr(CONNECTION_HANDLER_KEY).get();
        if (handler != null) {
            handler.onError(ctx, cause);
        }
        
        ctx.close();
    }
    
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        try {
            // Parse connection attributes from URL
            String uri = req.uri();
            logger.debug("WebSocket handshake request: {}", uri);
            
            ConnectionAttributes attributes = UrlParser.parseConnectionAttributes(uri);
            
            // Get remote address
            String remoteAddress = ctx.channel().remoteAddress().toString();
            
            // Create connection handler
            ConnectionHandler connectionHandler = connectionHandlerFactory.createConnectionHandler(
                    attributes, remoteAddress);
            
            // Store connection handler in channel attributes
            ctx.channel().attr(CONNECTION_HANDLER_KEY).set(connectionHandler);
            
            // Perform WebSocket handshake
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    getWebSocketLocation(req), null, true, 65536);
            handshaker = wsFactory.newHandshaker(req);
            
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req).addListener(future -> {
                    if (future.isSuccess()) {
                        // Register connection and notify handler
                        connectionRegistry.registerConnection(connectionHandler);
                        connectionHandler.onConnectionEstablished(ctx);
                        
                        logger.info("WebSocket connection established: {} {} from {}", 
                                   attributes.getType(), connectionHandler.getConnectionId(), remoteAddress);
                    } else {
                        logger.error("WebSocket handshake failed", future.cause());
                        ctx.close();
                    }
                });
            }
            
        } catch (Exception e) {
            logger.error("Error processing WebSocket handshake", e);
            ctx.close();
        }
    }
    
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        ConnectionHandler handler = ctx.channel().attr(CONNECTION_HANDLER_KEY).get();
        
        if (handler == null) {
            logger.warn("Received WebSocket frame but no connection handler found");
            ctx.close();
            return;
        }
        
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        
        if (frame instanceof TextWebSocketFrame) {
            handler.onTextMessage(ctx, (TextWebSocketFrame) frame);
            return;
        }
        
        if (frame instanceof BinaryWebSocketFrame) {
            logger.warn("Binary frames not supported from {}", handler.getConnectionId());
            return;
        }
        
        logger.warn("Unsupported frame type: {} from {}", 
                   frame.getClass().getSimpleName(), handler.getConnectionId());
    }
    
    private String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get("Host") + req.uri();
        return "ws://" + location;
    }
}