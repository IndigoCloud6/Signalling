package com.epicgames.pixelstreaming.signalling.server;

import com.epicgames.pixelstreaming.signalling.config.SignallingProperties;
import com.epicgames.pixelstreaming.signalling.factory.ConnectionHandlerFactory;
import com.epicgames.pixelstreaming.signalling.registry.ConnectionRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Netty-based WebSocket server that handles all connection types on a single port.
 * Routes connections based on URL parameters to appropriate handlers.
 */
@Component
public class NettyWebSocketServer {
    
    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketServer.class);
    
    private final SignallingProperties properties;
    private final ConnectionHandlerFactory connectionHandlerFactory;
    private final ConnectionRegistry connectionRegistry;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    @Autowired
    public NettyWebSocketServer(SignallingProperties properties,
                               ConnectionHandlerFactory connectionHandlerFactory,
                               ConnectionRegistry connectionRegistry) {
        this.properties = properties;
        this.connectionHandlerFactory = connectionHandlerFactory;
        this.connectionRegistry = connectionRegistry;
    }
    
    @PostConstruct
    public void start() throws InterruptedException {
        logger.info("Starting Netty WebSocket server on port {}", properties.getServer().getPort());
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // HTTP codec
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            
                            // CORS support if enabled
                            if (properties.getServer().isCorsEnabled()) {
                                CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin()
                                        .allowNullOrigin()
                                        .allowCredentials()
                                        .build();
                                pipeline.addLast(new CorsHandler(corsConfig));
                            }
                            
                            // WebSocket handler
                            pipeline.addLast(new WebSocketHandler(
                                    connectionHandlerFactory, 
                                    connectionRegistry, 
                                    properties));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // Bind and start to accept incoming connections
            ChannelFuture future = bootstrap.bind(properties.getServer().getPort()).sync();
            serverChannel = future.channel();
            
            logger.info("Netty WebSocket server started successfully on port {}", 
                       properties.getServer().getPort());
            
            // Start server in background thread
            new Thread(() -> {
                try {
                    serverChannel.closeFuture().sync();
                } catch (InterruptedException e) {
                    logger.warn("Server thread interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }, "netty-server").start();
            
        } catch (Exception e) {
            logger.error("Failed to start Netty WebSocket server", e);
            shutdown();
            throw e;
        }
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Netty WebSocket server");
        
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while closing server channel", e);
            Thread.currentThread().interrupt();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        logger.info("Netty WebSocket server shutdown complete");
    }
    
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
    
    public int getPort() {
        return properties.getServer().getPort();
    }
}