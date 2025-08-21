package com.epicgames.pixelstreaming.signalling.router;

import com.epicgames.pixelstreaming.signalling.connection.AbstractConnectionHandler;
import com.epicgames.pixelstreaming.signalling.connection.ConnectionHandler;
import com.epicgames.pixelstreaming.signalling.connection.PlayerConnectionHandler;
import com.epicgames.pixelstreaming.signalling.connection.SFUConnectionHandler;
import com.epicgames.pixelstreaming.signalling.connection.StreamerConnectionHandler;
import com.epicgames.pixelstreaming.signalling.registry.ConnectionRegistry;
import com.epicgames.pixelstreaming.signalling.registry.ConnectionRegistryListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Message router responsible for routing messages between different connection types.
 * Implements message forwarding logic for streamer-player communication.
 */
@Component
public class MessageRouter implements ConnectionRegistryListener {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageRouter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final ConnectionRegistry connectionRegistry;
    
    @Autowired
    public MessageRouter(ConnectionRegistry connectionRegistry) {
        this.connectionRegistry = connectionRegistry;
    }
    
    @PostConstruct
    public void init() {
        connectionRegistry.addListener(this);
    }
    
    @Override
    public void onConnectionAdded(ConnectionHandler connectionHandler) {
        logger.debug("Connection added to router: {} {}", 
                    connectionHandler.getAttributes().getType(), 
                    connectionHandler.getConnectionId());
    }
    
    @Override
    public void onConnectionRemoved(ConnectionHandler connectionHandler) {
        logger.debug("Connection removed from router: {} {}", 
                    connectionHandler.getAttributes().getType(), 
                    connectionHandler.getConnectionId());
    }
    
    /**
     * Routes a message from a player to a streamer.
     * 
     * @param playerId The player connection ID
     * @param streamerId The target streamer ID  
     * @param message The message to route
     */
    public void routePlayerToStreamer(String playerId, String streamerId, JsonNode message) {
        ConnectionHandler streamerHandler = findStreamerById(streamerId);
        if (streamerHandler != null) {
            // Add player ID to message for streamer identification
            if (message instanceof ObjectNode) {
                ((ObjectNode) message).put("playerId", playerId);
            }
            
            logger.debug("Routing message from player {} to streamer {}", playerId, streamerId);
            streamerHandler.sendMessage(getContextForConnection(streamerHandler), message);
        } else {
            logger.warn("Cannot route message from player {} to streamer {}: streamer not found", 
                       playerId, streamerId);
        }
    }
    
    /**
     * Routes a message from a streamer to a specific player.
     * 
     * @param streamerId The streamer connection ID
     * @param playerId The target player ID
     * @param message The message to route
     */
    public void routeStreamerToPlayer(String streamerId, String playerId, JsonNode message) {
        ConnectionHandler playerHandler = connectionRegistry.getConnection(playerId);
        if (playerHandler != null && playerHandler instanceof PlayerConnectionHandler) {
            // Remove playerId from message before sending to player
            if (message instanceof ObjectNode) {
                ((ObjectNode) message).remove("playerId");
            }
            
            logger.debug("Routing message from streamer {} to player {}", streamerId, playerId);
            playerHandler.sendMessage(getContextForConnection(playerHandler), message);
        } else {
            logger.warn("Cannot route message from streamer {} to player {}: player not found", 
                       streamerId, playerId);
        }
    }
    
    /**
     * Routes a message from a streamer to all subscribed players and SFUs.
     * 
     * @param streamerId The streamer connection ID
     * @param message The message to broadcast
     */
    public void broadcastFromStreamer(String streamerId, JsonNode message) {
        // Find all players subscribed to this streamer
        List<PlayerConnectionHandler> subscribedPlayers = connectionRegistry.getPlayersSubscribedTo(streamerId);
        
        // Find all SFUs subscribed to this streamer  
        List<SFUConnectionHandler> subscribedSFUs = connectionRegistry.getSFUsSubscribedTo(streamerId);
        
        logger.debug("Broadcasting message from streamer {} to {} players and {} SFUs", 
                    streamerId, subscribedPlayers.size(), subscribedSFUs.size());
        
        // Send to all subscribed players
        for (PlayerConnectionHandler player : subscribedPlayers) {
            JsonNode playerMessage = message.deepCopy();
            if (playerMessage instanceof ObjectNode) {
                ((ObjectNode) playerMessage).remove("playerId");
            }
            player.sendMessage(getContextForConnection(player), playerMessage);
        }
        
        // Send to all subscribed SFUs
        for (SFUConnectionHandler sfu : subscribedSFUs) {
            JsonNode sfuMessage = message.deepCopy();
            if (sfuMessage instanceof ObjectNode) {
                ((ObjectNode) sfuMessage).remove("playerId");
            }
            sfu.sendMessage(getContextForConnection(sfu), sfuMessage);
        }
    }
    
    /**
     * Gets a list of available streamers for a requesting client.
     * 
     * @return JSON array containing available streamer IDs
     */
    public ArrayNode getAvailableStreamers() {
        ArrayNode streamerIds = objectMapper.createArrayNode();
        
        // Add active streamers
        List<StreamerConnectionHandler> activeStreamers = connectionRegistry.getActiveStreamers();
        for (StreamerConnectionHandler streamer : activeStreamers) {
            // Extract streamer ID from connection (would need to be implemented in handler)
            // For now, use connection ID
            streamerIds.add(streamer.getConnectionId());
        }
        
        // Add active SFUs (they can also act as streamers)
        List<SFUConnectionHandler> activeSFUs = connectionRegistry.getActiveSFUs();
        for (SFUConnectionHandler sfu : activeSFUs) {
            streamerIds.add(sfu.getConnectionId());
        }
        
        return streamerIds;
    }
    
    /**
     * Disconnects a specific player as requested by a streamer.
     * 
     * @param streamerId The requesting streamer ID
     * @param playerId The player to disconnect
     * @param reason The reason for disconnection
     */
    public void disconnectPlayer(String streamerId, String playerId, String reason) {
        ConnectionHandler playerHandler = connectionRegistry.getConnection(playerId);
        if (playerHandler != null) {
            logger.info("Streamer {} requested disconnection of player {}: {}", 
                       streamerId, playerId, reason);
            
            // Send disconnect message to player
            ObjectNode disconnectMessage = objectMapper.createObjectNode();
            disconnectMessage.put("type", "disconnect");
            disconnectMessage.put("reason", reason);
            
            playerHandler.sendMessage(getContextForConnection(playerHandler), disconnectMessage);
            
            // Close the connection after sending the message
            ChannelHandlerContext ctx = getContextForConnection(playerHandler);
            if (ctx != null) {
                ctx.close();
            }
        } else {
            logger.warn("Cannot disconnect player {}: player not found", playerId);
        }
    }
    
    /**
     * Finds a streamer connection by streamer ID.
     * This searches both regular streamers and SFUs acting as streamers.
     * 
     * @param streamerId The streamer ID to search for
     * @return The streamer connection handler, or null if not found
     */
    private ConnectionHandler findStreamerById(String streamerId) {
        // First check regular streamers
        List<StreamerConnectionHandler> streamers = connectionRegistry.getActiveStreamers();
        for (StreamerConnectionHandler streamer : streamers) {
            if (streamerId.equals(streamer.getConnectionId())) {
                return streamer;
            }
        }
        
        // Then check SFUs acting as streamers
        List<SFUConnectionHandler> sfus = connectionRegistry.getActiveSFUs();
        for (SFUConnectionHandler sfu : sfus) {
            if (streamerId.equals(sfu.getConnectionId())) {
                return sfu;
            }
        }
        
        return null;
    }
    
    /**
     * Gets the channel context for a connection handler.
     * 
     * @param connectionHandler The connection handler
     * @return The channel context, or null if not available
     */
    private ChannelHandlerContext getContextForConnection(ConnectionHandler connectionHandler) {
        if (connectionHandler instanceof AbstractConnectionHandler) {
            return ((AbstractConnectionHandler) connectionHandler).getChannelContext();
        }
        return null;
    }
}