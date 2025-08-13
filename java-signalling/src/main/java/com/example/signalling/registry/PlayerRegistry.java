package com.example.signalling.registry;

import com.example.signalling.model.PlayerSession;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PlayerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PlayerRegistry.class);
    
    private final ConcurrentHashMap<String, PlayerSession> players = new ConcurrentHashMap<>();

    public PlayerRegistry(MeterRegistry meterRegistry) {
        Gauge.builder("signalling.players.current", this, PlayerRegistry::getPlayerCount)
                .description("Current number of connected players")
                .register(meterRegistry);
    }

    public void addPlayer(PlayerSession session) {
        players.put(session.getPlayerId(), session);
        logger.info("Player registered: {} -> streamer: {}", session.getPlayerId(), session.getStreamerId());
    }

    public void removePlayer(String playerId) {
        PlayerSession removed = players.remove(playerId);
        if (removed != null) {
            logger.info("Player removed: {}", playerId);
        }
    }

    public Optional<PlayerSession> getPlayer(String playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    public Collection<PlayerSession> getAllPlayers() {
        return players.values();
    }

    public List<PlayerSession> getPlayersForStreamer(String streamerId) {
        return players.values().stream()
                .filter(player -> streamerId.equals(player.getStreamerId()))
                .collect(Collectors.toList());
    }

    public boolean hasPlayer(String playerId) {
        return players.containsKey(playerId);
    }

    public double getPlayerCount() {
        return players.size();
    }

    public void removeStalePlayers(long staleTimeoutMs) {
        players.entrySet().removeIf(entry -> {
            boolean isStale = entry.getValue().isStale(staleTimeoutMs);
            if (isStale) {
                logger.info("Removing stale player: {}", entry.getKey());
            }
            return isStale;
        });
    }
}