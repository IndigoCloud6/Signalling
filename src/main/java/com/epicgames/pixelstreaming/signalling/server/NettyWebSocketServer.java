// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import com.epicgames.pixelstreaming.signalling.config.SignallingConfig;
import com.epicgames.pixelstreaming.signalling.handler.AbstractWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Netty-based WebSocket server for signalling connections.
 * Provides high-performance WebSocket handling for players, streamers, and SFUs.
 */
public class NettyWebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketServer.class);

    private final String name;
    private final String host;
    private final int port;
    private final Supplier<AbstractWebSocketHandler> handlerSupplier;
    private final SignallingConfig config;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyWebSocketServer(String name, String host, int port, 
                               Supplier<AbstractWebSocketHandler> handlerSupplier,
                               SignallingConfig config) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.handlerSupplier = handlerSupplier;
        this.config = config;
    }

    /**
     * Start the WebSocket server.
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                startServer();
            } catch (Exception e) {
                throw new RuntimeException("Failed to start " + name + " server", e);
            }
        });
    }

    /**
     * Stop the WebSocket server.
     */
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            try {
                stopServer();
            } catch (Exception e) {
                throw new RuntimeException("Failed to stop " + name + " server", e);
            }
        });
    }

    private void startServer() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new WebSocketServerInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(host, port).sync();
            serverChannel = future.channel();
            
            logger.info("{} server started on {}:{}", name, host, port);
            
            // Wait for the server socket to close
            serverChannel.closeFuture().sync();
            
        } finally {
            stopServer();
        }
    }

    private void stopServer() {
        logger.info("Stopping {} server", name);
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
        }
        
        logger.info("{} server stopped", name);
    }

    /**
     * Channel initializer for WebSocket connections.
     */
    private class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {
        
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            
            // HTTP codec
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(65536));
            
            // WebSocket handling
            pipeline.addLast(new WebSocketServerProtocolHandler("/", null, true, 
                config.getWebsocket().getMaxFrameSize()));
            
            // Idle state handling for connection timeout
            pipeline.addLast(new IdleStateHandler(
                config.getWebsocket().getConnectionTimeoutSeconds(),
                config.getWebsocket().getPingIntervalSeconds(),
                0,
                TimeUnit.SECONDS
            ));
            
            // Custom idle state handler
            pipeline.addLast(new IdleStateChannelHandler());
            
            // Application handler
            pipeline.addLast(handlerSupplier.get());
        }
    }

    /**
     * Handler for idle state events (ping/pong and connection timeout).
     */
    private static class IdleStateChannelHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                
                switch (event.state()) {
                    case READER_IDLE:
                        logger.debug("Connection {} reader idle, closing", ctx.channel().remoteAddress());
                        ctx.close();
                        break;
                        
                    case WRITER_IDLE:
                        logger.debug("Sending ping to connection {}", ctx.channel().remoteAddress());
                        ctx.writeAndFlush(new PingWebSocketFrame());
                        break;
                        
                    case ALL_IDLE:
                        logger.debug("Connection {} all idle, closing", ctx.channel().remoteAddress());
                        ctx.close();
                        break;
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    // Getters
    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isRunning() { 
        return serverChannel != null && serverChannel.isActive(); 
    }
}