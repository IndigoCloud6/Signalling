# Java Signalling Server

A Java 17 Spring Boot implementation of a WebRTC signalling server using Spring WebFlux (Netty) and WebSocket support.

## Overview

This Java implementation provides feature parity with the TypeScript signalling server for basic WebRTC signalling flows. It handles WebSocket connections from streamers and players, routing offer/answer/ICE candidate messages between them, and provides observability through metrics and structured logging.

## Requirements

- Java 17 or higher
- Maven 3.6+ (or use provided Maven wrapper)

## Build & Run

### Using Maven Wrapper (Recommended)

```bash
# Build the project
./mvnw clean compile

# Run tests
./mvnw test

# Start the server
./mvnw spring-boot:run
```

### Using System Maven

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Start the server
mvn spring-boot:run
```

The server will start on port 9090 by default.

## Configuration

The application can be configured via `application.yml`:

```yaml
server:
  port: 9090

signalling:
  staleTimeoutMs: 60000       # Session timeout in milliseconds
  cleanupIntervalMs: 10000    # Cleanup interval in milliseconds
  rateLimit:
    windowSeconds: 10         # Rate limit window in seconds
    maxMessages: 100          # Max messages per window per session

management:
  endpoints:
    web:
      exposure:
        include: health, prometheus
```

## Endpoints

### WebSocket
- `ws://localhost:9090/ws` - Main WebSocket endpoint for signalling

### HTTP
- `GET /actuator/health` - Health check endpoint
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /health-check` - Simple health check

## Message Types & Examples

The server supports the following JSON message types over WebSocket:

### Register Streamer
```json
{ "type": "registerStreamer", "streamerId": "s1" }
```
Note: If `streamerId` is omitted or empty, server generates a UUID.

### Register Player
```json
{ "type": "registerPlayer", "playerId": "p1", "streamerId": "s1" }
```

### WebRTC Signalling Messages

#### Offer
```json
{ "type": "offer", "fromPlayer": "p1", "toStreamer": "s1", "sdp": "..." }
```

#### Answer
```json
{ "type": "answer", "fromStreamer": "s1", "toPlayer": "p1", "sdp": "..." }
```

#### ICE Candidate
```json
{ 
  "type": "iceCandidate", 
  "from": "p1", 
  "to": "s1", 
  "candidate": {
    "candidate": "...",
    "sdpMid": "0",
    "sdpMLineIndex": 0
  }
}
```

#### Ping
```json
{ "type": "ping" }
```

#### Disconnect Player
```json
{ "type": "disconnectPlayer", "playerId": "p1" }
```

### Error Response
```json
{ "type": "error", "error": "Error message" }
```

### Peer Disconnected Notification
```json
{ "type": "peerDisconnected", "peerId": "p1" }
```

## Message Flow Examples

### Basic Signalling Flow
1. Streamer connects and registers: `{"type": "registerStreamer", "streamerId": "streamer1"}`
2. Player connects and registers: `{"type": "registerPlayer", "playerId": "player1", "streamerId": "streamer1"}`
3. Player sends offer: `{"type": "offer", "fromPlayer": "player1", "toStreamer": "streamer1", "sdp": "..."}`
4. Server forwards offer to streamer
5. Streamer sends answer: `{"type": "answer", "fromStreamer": "streamer1", "toPlayer": "player1", "sdp": "..."}`
6. Server forwards answer to player
7. Both sides exchange ICE candidates as needed

### Error Handling
- Invalid JSON → `{"type": "error", "error": "Invalid JSON format"}`
- Unknown message type → `{"type": "error", "error": "Unknown message type: ..."}`
- Target not found → `{"type": "error", "error": "Streamer/Player not found: ..."}`
- Rate limit exceeded → `{"type": "error", "error": "Rate limit exceeded"}`

## Features

### Rate Limiting
- Per-session sliding window rate limiting (default: 100 messages per 10 seconds)
- Automatic rate limit violation logging and metrics

### Session Management
- Automatic cleanup of stale connections
- Last-activity tracking for all sessions
- Graceful disconnection handling with peer notifications

### Observability

#### Structured Logging
- JSON and console logging formats
- MDC context for request tracing
- Direction tracking (incoming/outgoing messages)
- Session and message type tracking

#### Metrics (Prometheus)
- `signalling.streamers.current` - Current number of connected streamers
- `signalling.players.current` - Current number of connected players
- `signalling.messages.forwarded` - Total messages forwarded between peers
- `signalling.message.handling.duration` - Message processing latency
- `signalling.rate_limit.exceeded` - Rate limit violations

### Error Handling
- Malformed JSON detection and reporting
- Unknown message type handling
- Connection error recovery
- Comprehensive error logging

## Development & Testing

### Running Tests
```bash
./mvnw test
```

The test suite includes:
- Integration tests for complete signalling flows
- WebSocket connection testing
- Error handling validation
- Rate limiting verification

### Key Test Scenarios
- Streamer registration → Player registration → Offer forwarding
- Answer forwarding from streamer to player
- ICE candidate exchange
- Error handling for invalid JSON
- Error handling for non-existent targets

### Development Features
- Hot reload during development
- Detailed debug logging for WebSocket frames
- Comprehensive test coverage for core signalling logic

## Architecture

### Core Components
- `SignallingWebSocketHandler` - WebSocket connection management
- `MessageRouter` - Message parsing and routing
- `RoutingService` - Business logic for message forwarding
- `StreamerRegistry` / `PlayerRegistry` - Session management
- `RateLimitService` - Rate limiting implementation
- `CleanupService` - Stale session cleanup

### Technology Stack
- **Spring Boot 3.2.1** - Application framework
- **Spring WebFlux** - Reactive web framework
- **Reactor Netty** - Embedded server and WebSocket support
- **Jackson** - JSON serialization/deserialization
- **Micrometer** - Metrics collection
- **SLF4J + Logback** - Structured logging

### Threading Model
- Non-blocking reactive processing using Project Reactor
- Netty event loop for WebSocket handling
- Scheduled tasks for cleanup operations

## Deployment

### Docker (Future)
The application is designed to be easily containerizable with Spring Boot's built-in Docker support.

### Production Considerations
- Configure appropriate rate limits for your use case
- Monitor metrics for performance optimization
- Set up proper log aggregation for structured logs
- Use external health checks via `/actuator/health`

## Limitations & Future Enhancements

### Current Limitations
- Single instance only (no clustering)
- In-memory session storage
- JSON-only message format

### Future Enhancements (Not in this version)
- Redis pub-sub for clustering
- Authentication/authorization
- SFU integration
- Horizontal scalability
- Protobuf message encoding
- Persistent session storage