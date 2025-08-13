package com.example.signalling.websocket;

import com.example.signalling.service.MessageRouter;
import com.example.signalling.service.RateLimitService;
import com.example.signalling.service.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
public class SignallingWebSocketHandler implements WebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(SignallingWebSocketHandler.class);
    
    private final MessageRouter messageRouter;
    private final RateLimitService rateLimitService;
    private final RoutingService routingService;

    public SignallingWebSocketHandler(MessageRouter messageRouter, 
                                    RateLimitService rateLimitService,
                                    RoutingService routingService) {
        this.messageRouter = messageRouter;
        this.rateLimitService = rateLimitService;
        this.routingService = routingService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        logger.info("WebSocket connection established: {}", session.getId());
        
        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(message -> {
                    String sessionId = session.getId();
                    
                    // Check rate limiting
                    if (!rateLimitService.isAllowed(sessionId)) {
                        logger.warn("Rate limit exceeded for session: {}", sessionId);
                        return sendRateLimitError(session);
                    }
                    
                    return messageRouter.routeMessage(message, session);
                })
                .onErrorResume(throwable -> {
                    logger.error("Error handling WebSocket message for session: {}", session.getId(), throwable);
                    return Mono.empty();
                })
                .doOnComplete(() -> {
                    logger.info("WebSocket connection closed: {}", session.getId());
                    handleConnectionClosed(session);
                })
                .doOnCancel(() -> {
                    logger.info("WebSocket connection cancelled: {}", session.getId());
                    handleConnectionClosed(session);
                })
                .doOnError(throwable -> {
                    logger.error("WebSocket connection error for session: {}", session.getId(), throwable);
                    handleConnectionClosed(session);
                })
                .then();
    }

    private void handleConnectionClosed(WebSocketSession session) {
        String sessionId = session.getId();
        rateLimitService.removeSession(sessionId);
        routingService.handleSessionClosed(session);
    }

    private Mono<Void> sendRateLimitError(WebSocketSession session) {
        String errorJson = "{\"type\":\"error\",\"error\":\"Rate limit exceeded\"}";
        return session.send(Mono.just(session.textMessage(errorJson)));
    }
}