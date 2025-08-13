package com.example.signalling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "signalling.staleTimeoutMs=5000",
    "signalling.cleanupIntervalMs=1000",
    "signalling.rateLimit.windowSeconds=10",
    "signalling.rateLimit.maxMessages=100"
})
class SignallingIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testStreamerRegistrationAndPlayerOfferForward() throws Exception {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        URI uri = URI.create("ws://localhost:" + port + "/ws");

        // Latch to coordinate test execution
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        // Connect streamer
        Sinks.Many<String> streamerInput = Sinks.many().multicast().onBackpressureBuffer();
        
        client.execute(uri, session -> {
            // Register streamer
            String registerStreamerMsg = "{\"type\":\"registerStreamer\",\"streamerId\":\"test-streamer-1\"}";
            
            // Listen for messages
            Mono<Void> input = session.send(
                streamerInput.asFlux()
                    .startWith(registerStreamerMsg)
                    .map(session::textMessage)
            );
            
            Mono<Void> output = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> {
                    receivedMessage.set(message);
                    messageLatch.countDown();
                })
                .then();
            
            return Mono.zip(input, output).then();
        }).subscribe();

        // Give streamer time to connect
        Thread.sleep(500);

        // Connect player and send offer
        client.execute(uri, session -> {
            String registerPlayerMsg = "{\"type\":\"registerPlayer\",\"playerId\":\"test-player-1\",\"streamerId\":\"test-streamer-1\"}";
            String offerMsg = "{\"type\":\"offer\",\"fromPlayer\":\"test-player-1\",\"toStreamer\":\"test-streamer-1\",\"sdp\":\"test-sdp-data\"}";
            
            return session.send(
                Flux.just(registerPlayerMsg, offerMsg)
                    .map(session::textMessage)
            );
        }).subscribe();

        // Wait for offer to be forwarded to streamer
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive forwarded offer within 5 seconds");
        
        // Verify the offer was forwarded correctly
        String receivedJson = receivedMessage.get();
        assertNotNull(receivedJson);
        
        JsonNode received = objectMapper.readTree(receivedJson);
        assertEquals("offer", received.get("type").asText());
        assertEquals("test-player-1", received.get("fromPlayer").asText());
        assertEquals("test-streamer-1", received.get("toStreamer").asText());
        assertEquals("test-sdp-data", received.get("sdp").asText());
    }

    @Test
    void testPlayerRegistrationWithInvalidStreamer() throws Exception {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        URI uri = URI.create("ws://localhost:" + port + "/ws");

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        // Connect player to non-existent streamer
        client.execute(uri, session -> {
            String registerPlayerMsg = "{\"type\":\"registerPlayer\",\"playerId\":\"test-player-2\",\"streamerId\":\"non-existent-streamer\"}";
            
            Mono<Void> input = session.send(Mono.just(session.textMessage(registerPlayerMsg)));
            
            Mono<Void> output = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> {
                    errorMessage.set(message);
                    errorLatch.countDown();
                })
                .then();
            
            return Mono.zip(input, output).then();
        }).subscribe();

        // Wait for error response
        assertTrue(errorLatch.await(3, TimeUnit.SECONDS), "Should receive error within 3 seconds");
        
        // Verify error message
        String receivedJson = errorMessage.get();
        assertNotNull(receivedJson);
        
        JsonNode received = objectMapper.readTree(receivedJson);
        assertEquals("error", received.get("type").asText());
        assertTrue(received.get("error").asText().contains("Streamer not found"));
    }

    @Test
    void testAnswerForwarding() throws Exception {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        URI uri = URI.create("ws://localhost:" + port + "/ws");

        CountDownLatch answerLatch = new CountDownLatch(1);
        AtomicReference<String> receivedAnswer = new AtomicReference<>();

        // Connect player first
        Sinks.Many<String> playerInput = Sinks.many().multicast().onBackpressureBuffer();
        
        client.execute(uri, session -> {
            String registerPlayerMsg = "{\"type\":\"registerPlayer\",\"playerId\":\"test-player-3\",\"streamerId\":\"test-streamer-2\"}";
            
            Mono<Void> input = session.send(
                playerInput.asFlux()
                    .startWith(registerPlayerMsg)
                    .map(session::textMessage)
            );
            
            Mono<Void> output = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> {
                    receivedAnswer.set(message);
                    answerLatch.countDown();
                })
                .then();
            
            return Mono.zip(input, output).then();
        }).subscribe();

        // Give player time to connect
        Thread.sleep(500);

        // Connect streamer
        client.execute(uri, session -> {
            String registerStreamerMsg = "{\"type\":\"registerStreamer\",\"streamerId\":\"test-streamer-2\"}";
            String answerMsg = "{\"type\":\"answer\",\"fromStreamer\":\"test-streamer-2\",\"toPlayer\":\"test-player-3\",\"sdp\":\"answer-sdp-data\"}";
            
            return session.send(
                Flux.just(registerStreamerMsg, answerMsg)
                    .map(session::textMessage)
            );
        }).subscribe();

        // Wait for answer to be forwarded to player
        assertTrue(answerLatch.await(5, TimeUnit.SECONDS), "Should receive forwarded answer within 5 seconds");
        
        // Verify the answer was forwarded correctly
        String receivedJson = receivedAnswer.get();
        assertNotNull(receivedJson);
        
        JsonNode received = objectMapper.readTree(receivedJson);
        assertEquals("answer", received.get("type").asText());
        assertEquals("test-streamer-2", received.get("fromStreamer").asText());
        assertEquals("test-player-3", received.get("toPlayer").asText());
        assertEquals("answer-sdp-data", received.get("sdp").asText());
    }

    @Test
    void testIceCandidateForwarding() throws Exception {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        URI uri = URI.create("ws://localhost:" + port + "/ws");

        CountDownLatch iceLatch = new CountDownLatch(1);
        AtomicReference<String> receivedIce = new AtomicReference<>();

        // Connect streamer first
        Sinks.Many<String> streamerInput = Sinks.many().multicast().onBackpressureBuffer();
        
        client.execute(uri, session -> {
            String registerStreamerMsg = "{\"type\":\"registerStreamer\",\"streamerId\":\"test-streamer-3\"}";
            
            Mono<Void> input = session.send(
                streamerInput.asFlux()
                    .startWith(registerStreamerMsg)
                    .map(session::textMessage)
            );
            
            Mono<Void> output = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> {
                    receivedIce.set(message);
                    iceLatch.countDown();
                })
                .then();
            
            return Mono.zip(input, output).then();
        }).subscribe();

        // Give streamer time to connect
        Thread.sleep(500);

        // Connect player and send ICE candidate
        client.execute(uri, session -> {
            String registerPlayerMsg = "{\"type\":\"registerPlayer\",\"playerId\":\"test-player-4\",\"streamerId\":\"test-streamer-3\"}";
            String iceCandidateMsg = "{\"type\":\"iceCandidate\",\"from\":\"test-player-4\",\"to\":\"test-streamer-3\",\"candidate\":{\"candidate\":\"test-ice-candidate\",\"sdpMid\":\"0\",\"sdpMLineIndex\":0}}";
            
            return session.send(
                Flux.just(registerPlayerMsg, iceCandidateMsg)
                    .map(session::textMessage)
            );
        }).subscribe();

        // Wait for ICE candidate to be forwarded to streamer
        assertTrue(iceLatch.await(5, TimeUnit.SECONDS), "Should receive forwarded ICE candidate within 5 seconds");
        
        // Verify the ICE candidate was forwarded correctly
        String receivedJson = receivedIce.get();
        assertNotNull(receivedJson);
        
        JsonNode received = objectMapper.readTree(receivedJson);
        assertEquals("iceCandidate", received.get("type").asText());
        assertEquals("test-player-4", received.get("from").asText());
        assertEquals("test-streamer-3", received.get("to").asText());
        assertEquals("test-ice-candidate", received.get("candidate").get("candidate").asText());
    }

    @Test
    void testInvalidJsonHandling() throws Exception {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        URI uri = URI.create("ws://localhost:" + port + "/ws");

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        // Send invalid JSON
        client.execute(uri, session -> {
            String invalidJson = "{invalid json}";
            
            Mono<Void> input = session.send(Mono.just(session.textMessage(invalidJson)));
            
            Mono<Void> output = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> {
                    errorMessage.set(message);
                    errorLatch.countDown();
                })
                .then();
            
            return Mono.zip(input, output).then();
        }).subscribe();

        // Wait for error response
        assertTrue(errorLatch.await(3, TimeUnit.SECONDS), "Should receive error within 3 seconds");
        
        // Verify error message
        String receivedJson = errorMessage.get();
        assertNotNull(receivedJson);
        
        JsonNode received = objectMapper.readTree(receivedJson);
        assertEquals("error", received.get("type").asText());
        assertTrue(received.get("error").asText().contains("Invalid JSON format"));
    }
}