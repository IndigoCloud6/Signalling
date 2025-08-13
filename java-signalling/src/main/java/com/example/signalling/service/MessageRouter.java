package com.example.signalling.service;

import com.example.signalling.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Service
public class MessageRouter {
    private static final Logger logger = LoggerFactory.getLogger(MessageRouter.class);
    
    private final RoutingService routingService;
    private final ObjectMapper objectMapper;

    public MessageRouter(RoutingService routingService, ObjectMapper objectMapper) {
        this.routingService = routingService;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> routeMessage(String messageJson, WebSocketSession session) {
        try {
            MDC.put("direction", "incoming");
            MDC.put("sessionId", session.getId());
            
            SignallingMessage message = objectMapper.readValue(messageJson, SignallingMessage.class);
            String messageType = message.getType();
            
            MDC.put("messageType", messageType);
            logger.info("Received message: {}", messageType);
            
            Mono<Void> result = switch (messageType) {
                case "registerStreamer" -> routingService.handleRegisterStreamer((RegisterStreamerMessage) message, session);
                case "registerPlayer" -> routingService.handleRegisterPlayer((RegisterPlayerMessage) message, session);
                case "offer" -> routingService.handleOffer((OfferMessage) message, session);
                case "answer" -> routingService.handleAnswer((AnswerMessage) message, session);
                case "iceCandidate" -> routingService.handleIceCandidate((IceCandidateMessage) message, session);
                case "ping" -> routingService.handlePing(session);
                case "disconnectPlayer" -> routingService.handleDisconnectPlayer((DisconnectPlayerMessage) message, session);
                default -> {
                    logger.warn("Unknown message type: {}", messageType);
                    yield sendErrorMessage(session, "Unknown message type: " + messageType);
                }
            };
            
            return result.doFinally(signal -> {
                MDC.remove("direction");
                MDC.remove("sessionId");
                MDC.remove("messageType");
            });
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse message: {}", messageJson, e);
            return sendErrorMessage(session, "Invalid JSON format")
                    .doFinally(signal -> {
                        MDC.remove("direction");
                        MDC.remove("sessionId");
                        MDC.remove("messageType");
                    });
        } catch (Exception e) {
            logger.error("Error processing message: {}", messageJson, e);
            return sendErrorMessage(session, "Internal server error")
                    .doFinally(signal -> {
                        MDC.remove("direction");
                        MDC.remove("sessionId");
                        MDC.remove("messageType");
                    });
        }
    }

    private Mono<Void> sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            ErrorMessage error = new ErrorMessage(errorMessage);
            String json = objectMapper.writeValueAsString(error);
            return session.send(Mono.just(session.textMessage(json)));
        } catch (JsonProcessingException e) {
            logger.error("Failed to send error message", e);
            return Mono.empty();
        }
    }
}