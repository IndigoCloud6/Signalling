package com.example.signalling.service;

import com.example.signalling.config.SignallingProperties;
import com.example.signalling.registry.PlayerRegistry;
import com.example.signalling.registry.StreamerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CleanupService {
    private static final Logger logger = LoggerFactory.getLogger(CleanupService.class);
    
    private final StreamerRegistry streamerRegistry;
    private final PlayerRegistry playerRegistry;
    private final SignallingProperties properties;

    public CleanupService(StreamerRegistry streamerRegistry, 
                         PlayerRegistry playerRegistry,
                         SignallingProperties properties) {
        this.streamerRegistry = streamerRegistry;
        this.playerRegistry = playerRegistry;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${signalling.cleanupIntervalMs}")
    public void cleanupStaleConnections() {
        logger.debug("Running cleanup task for stale connections");
        
        long staleTimeoutMs = properties.staleTimeoutMs();
        
        int streamersBefore = (int) streamerRegistry.getStreamerCount();
        int playersBefore = (int) playerRegistry.getPlayerCount();
        
        streamerRegistry.removeStaleStreamers(staleTimeoutMs);
        playerRegistry.removeStalePlayers(staleTimeoutMs);
        
        int streamersAfter = (int) streamerRegistry.getStreamerCount();
        int playersAfter = (int) playerRegistry.getPlayerCount();
        
        int removedStreamers = streamersBefore - streamersAfter;
        int removedPlayers = playersBefore - playersAfter;
        
        if (removedStreamers > 0 || removedPlayers > 0) {
            logger.info("Cleanup completed. Removed {} stale streamers and {} stale players", 
                       removedStreamers, removedPlayers);
        }
    }
}