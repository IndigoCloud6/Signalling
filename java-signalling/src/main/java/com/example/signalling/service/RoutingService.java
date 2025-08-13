package com.example.signalling.service;

import com.example.signalling.model.*;
import com.example.signalling.registry.PlayerRegistry;
import com.example.signalling.registry.StreamerRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class RoutingService {
    private static final Logger logger = LoggerFactory.getLogger(RoutingService.class);
    
    private final StreamerRegistry streamerRegistry;
    private final PlayerRegistry playerRegistry;
    private final ObjectMapper objectMapper;
    private final Counter messagesForwardedCounter;
    private final Timer messageHandlingTimer;

    public RoutingService(StreamerRegistry streamerRegistry, 
                         PlayerRegistry playerRegistry,
                         ObjectMapper objectMapper,
                         MeterRegistry meterRegistry) {
        this.streamerRegistry = streamerRegistry;
        this.playerRegistry = playerRegistry;
        this.objectMapper = objectMapper;
        this.messagesForwardedCounter = Counter.builder("signalling.messages.forwarded")
                .description("Number of messages forwarded between peers")
                .register(meterRegistry);
        this.messageHandlingTimer = Timer.builder("signalling.message.handling.duration")
                .description("Time spent handling signalling messages")
                .register(meterRegistry);
    }

    public Mono<Void> handleRegisterStreamer(RegisterStreamerMessage message, WebSocketSession session) {
        return Mono.fromRunnable(() -> {
            Timer.Sample sample = Timer.start();
            try {
                String streamerId = message.getStreamerId();
                if (streamerId == null || streamerId.trim().isEmpty()) {
                    streamerId = UUID.randomUUID().toString();
                }

                MDC.put("streamerId", streamerId);
                logger.info("Registering streamer: {}", streamerId);

                StreamerSession streamerSession = new StreamerSession(streamerId, session);
                streamerRegistry.addStreamer(streamerSession);
                MDC.clear();
            } finally {
                sample.stop(messageHandlingTimer);
            }
        });
    }

    public Mono<Void> handleRegisterPlayer(RegisterPlayerMessage message, WebSocketSession session) {
        return Mono.fromRunnable(() -> {
            Timer.Sample sample = Timer.start();
            try {
                String playerId = message.getPlayerId();
                String streamerId = message.getStreamerId();

                MDC.put("playerId", playerId);
                MDC.put("streamerId", streamerId);

                if (!streamerRegistry.hasStreamer(streamerId)) {
                    logger.warn("Player {} tried to register with non-existent streamer: {}", playerId, streamerId);
                    sendErrorMessage(session, "Streamer not found: " + streamerId);
                    MDC.clear();
                    return;
                }

                logger.info("Registering player: {} -> streamer: {}", playerId, streamerId);
                PlayerSession playerSession = new PlayerSession(playerId, streamerId, session);
                playerRegistry.addPlayer(playerSession);
                MDC.clear();
            } finally {
                sample.stop(messageHandlingTimer);
            }
        });
    }

    public Mono<Void> handleOffer(OfferMessage message, WebSocketSession senderSession) {
        return Mono.fromRunnable(() -> {
            Timer.Sample sample = Timer.start();
            try {
                String toStreamer = message.getToStreamer();
                
                MDC.put("fromPlayer", message.getFromPlayer());
                MDC.put("toStreamer", toStreamer);
                logger.info("Forwarding offer from player {} to streamer {}", message.getFromPlayer(), toStreamer);

                streamerRegistry.getStreamer(toStreamer).ifPresentOrElse(
                    streamerSession -> {
                        streamerSession.updateLastActive();
                        sendMessage(streamerSession.getWebSocketSession(), message);
                        messagesForwardedCounter.increment();
                    },
                    () -> {
                        logger.warn("Offer target streamer not found: {}", toStreamer);
                        sendErrorMessage(senderSession, "Streamer not found: " + toStreamer);
                    }
                );
                MDC.clear();
            } finally {
                sample.stop(messageHandlingTimer);
            }
        });
    }

    public Mono<Void> handleAnswer(AnswerMessage message, WebSocketSession senderSession) {
        return Mono.fromRunnable(() -> {
            Timer.Sample sample = Timer.start();
            try {
                String toPlayer = message.getToPlayer();
                
                MDC.put("fromStreamer", message.getFromStreamer());
                MDC.put("toPlayer", toPlayer);
                logger.info("Forwarding answer from streamer {} to player {}", message.getFromStreamer(), toPlayer);

                playerRegistry.getPlayer(toPlayer).ifPresentOrElse(
                    playerSession -> {
                        playerSession.updateLastActive();
                        sendMessage(playerSession.getWebSocketSession(), message);
                        messagesForwardedCounter.increment();
                    },
                    () -> {
                        logger.warn("Answer target player not found: {}", toPlayer);
                        sendErrorMessage(senderSession, "Player not found: " + toPlayer);
                    }
                );
                MDC.clear();
            } finally {
                sample.stop(messageHandlingTimer);
            }
        });
    }

    public Mono<Void> handleIceCandidate(IceCandidateMessage message, WebSocketSession senderSession) {
        return Mono.fromRunnable(() -> {
            Timer.Sample sample = Timer.start();
            try {
                String from = message.getFrom();
                String to = message.getTo();
                
                MDC.put("from", from);
                MDC.put("to", to);
                logger.info("Forwarding ICE candidate from {} to {}", from, to);

                // Try as player -> streamer first
                if (streamerRegistry.getStreamer(to).isPresent()) {
                    StreamerSession streamerSession = streamerRegistry.getStreamer(to).get();
                    streamerSession.updateLastActive();
                    sendMessage(streamerSession.getWebSocketSession(), message);
                    messagesForwardedCounter.increment();
                } 
                // Try as streamer -> player
                else if (playerRegistry.getPlayer(to).isPresent()) {
                    PlayerSession playerSession = playerRegistry.getPlayer(to).get();
                    playerSession.updateLastActive();
                    sendMessage(playerSession.getWebSocketSession(), message);
                    messagesForwardedCounter.increment();
                } 
                else {
                    logger.warn("ICE candidate target not found: {}", to);
                    sendErrorMessage(senderSession, "Target not found: " + to);
                }
                MDC.clear();
            } finally {
                sample.stop(messageHandlingTimer);
            }
        });
    }

    public Mono<Void> handleDisconnectPlayer(DisconnectPlayerMessage message, WebSocketSession senderSession) {
        return Mono.fromRunnable(() -> {
            Timer.Sample sample = Timer.start();
            try {
                String playerId = message.getPlayerId();
                
                MDC.put("playerId", playerId);
                logger.info("Disconnecting player: {}", playerId);

                playerRegistry.getPlayer(playerId).ifPresent(playerSession -> {
                    // Notify the streamer about player disconnection
                    String streamerId = playerSession.getStreamerId();
                    streamerRegistry.getStreamer(streamerId).ifPresent(streamerSession -> {
                        PeerDisconnectedMessage peerDisconnected = new PeerDisconnectedMessage(playerId);
                        sendMessage(streamerSession.getWebSocketSession(), peerDisconnected);
                    });
                    
                    playerRegistry.removePlayer(playerId);
                });
                MDC.clear();
            } finally {
                sample.stop(messageHandlingTimer);
            }
        });
    }

    public Mono<Void> handlePing(WebSocketSession session) {
        return Mono.fromRunnable(() -> {
            Timer.Sample sample = Timer.start();
            try {
                // Update last active timestamp for the session
                String sessionId = session.getId();
                
                // Try to find in streamers first
                streamerRegistry.getAllStreamers().stream()
                    .filter(s -> s.getWebSocketSession().getId().equals(sessionId))
                    .findFirst()
                    .ifPresent(StreamerSession::updateLastActive);
                
                // Try to find in players
                playerRegistry.getAllPlayers().stream()
                    .filter(p -> p.getWebSocketSession().getId().equals(sessionId))
                    .findFirst()
                    .ifPresent(PlayerSession::updateLastActive);
                
                logger.debug("Ping received from session: {}", sessionId);
            } finally {
                sample.stop(messageHandlingTimer);
            }
        });
    }

    public void handleSessionClosed(WebSocketSession session) {
        String sessionId = session.getId();
        
        // Remove from streamers
        streamerRegistry.getAllStreamers().stream()
            .filter(s -> s.getWebSocketSession().getId().equals(sessionId))
            .findFirst()
            .ifPresent(streamerSession -> {
                String streamerId = streamerSession.getStreamerId();
                
                // Notify all connected players about streamer disconnection
                List<PlayerSession> connectedPlayers = playerRegistry.getPlayersForStreamer(streamerId);
                for (PlayerSession playerSession : connectedPlayers) {
                    PeerDisconnectedMessage peerDisconnected = new PeerDisconnectedMessage(streamerId);
                    sendMessage(playerSession.getWebSocketSession(), peerDisconnected);
                }
                
                streamerRegistry.removeStreamer(streamerId);
                logger.info("Streamer disconnected: {}", streamerId);
            });
        
        // Remove from players
        playerRegistry.getAllPlayers().stream()
            .filter(p -> p.getWebSocketSession().getId().equals(sessionId))
            .findFirst()
            .ifPresent(playerSession -> {
                String playerId = playerSession.getPlayerId();
                String streamerId = playerSession.getStreamerId();
                
                // Notify the streamer about player disconnection
                streamerRegistry.getStreamer(streamerId).ifPresent(streamerSession -> {
                    PeerDisconnectedMessage peerDisconnected = new PeerDisconnectedMessage(playerId);
                    sendMessage(streamerSession.getWebSocketSession(), peerDisconnected);
                });
                
                playerRegistry.removePlayer(playerId);
                logger.info("Player disconnected: {}", playerId);
            });
    }

    private void sendMessage(WebSocketSession session, SignallingMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            WebSocketMessage wsMessage = session.textMessage(json);
            session.send(Mono.just(wsMessage)).subscribe();
            
            MDC.put("direction", "outgoing");
            MDC.put("messageType", message.getType());
            logger.info("Sent message: {}", message.getType());
            MDC.remove("direction");
            MDC.remove("messageType");
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message", e);
        }
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        sendMessage(session, new ErrorMessage(errorMessage));
    }
}