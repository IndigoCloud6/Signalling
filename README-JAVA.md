# Pixel Streaming Signalling Server (Java)

A high-performance Java implementation of the Pixel Streaming signalling server using Netty and Spring Boot. This server provides WebSocket-based signalling for Pixel Streaming applications with support for players, streamers, and SFU (Selective Forwarding Unit) connections.

## Features

- **High-Performance WebSocket Server**: Built on Netty for handling 1000+ concurrent connections
- **Protocol Compatible**: Maintains compatibility with existing Pixel Streaming applications
- **Multiple Connection Types**: Support for player, streamer, and SFU connections
- **REST API**: HTTP endpoints for health checks, statistics, and configuration
- **Security**: CORS support, rate limiting, and optional authentication
- **Monitoring**: Micrometer metrics with Prometheus export
- **Structured Logging**: JSON and text logging with Logback
- **Configuration Management**: YAML configuration with environment variable support
- **Docker Ready**: Containerization support for easy deployment

## Technology Stack

- **Java 17+**: Modern Java features and performance
- **Spring Boot 3.x**: Dependency injection, configuration management, and auto-configuration
- **Netty 4.x**: High-performance asynchronous network application framework
- **Jackson**: JSON processing and serialization
- **Micrometer**: Application metrics and monitoring
- **Logback**: Logging framework with structured output
- **JUnit 5**: Testing framework
- **Maven**: Build and dependency management

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Building

```bash
# Clone the repository
git clone <repository-url>
cd Signalling

# Build the project
mvn clean package

# Run the application
java -jar target/signalling-0.1.2.jar
```

### Running with Maven

```bash
mvn spring-boot:run
```

## Configuration

The application can be configured through `application.yml` or environment variables.

### Default Ports

- **Player WebSocket**: 8889
- **Streamer WebSocket**: 8888
- **SFU WebSocket**: 8890
- **HTTP API**: 8080

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SIGNALLING_HOST` | Server bind address | `0.0.0.0` |
| `SIGNALLING_PLAYER_PORT` | Player WebSocket port | `8889` |
| `SIGNALLING_STREAMER_PORT` | Streamer WebSocket port | `8888` |
| `SIGNALLING_SFU_PORT` | SFU WebSocket port | `8890` |
| `SIGNALLING_HTTP_PORT` | HTTP API port | `8080` |
| `SIGNALLING_MAX_SUBSCRIBERS` | Max players per streamer | `100` |
| `SIGNALLING_ENABLE_SFU` | Enable SFU support | `true` |
| `SIGNALLING_ENABLE_AUTH` | Enable authentication | `false` |
| `SIGNALLING_ENABLE_CORS` | Enable CORS | `true` |
| `SIGNALLING_LOG_LEVEL` | Logging level | `INFO` |

### Example Configuration

```yaml
signalling:
  server:
    host: 0.0.0.0
    player-port: 8889
    streamer-port: 8888
    sfu-port: 8890
    http-port: 8080
    max-subscribers: 100
    enable-sfu: true

  security:
    enable-auth: false
    enable-cors: true
    rate-limit-per-minute: 60

  websocket:
    max-frame-size: 65536
    ping-interval-seconds: 30
    connection-timeout-seconds: 60
```

## API Endpoints

### Health Check
```
GET /api/health
```
Returns the health status of the signalling server.

### Statistics
```
GET /api/stats
```
Returns connection statistics and server metrics.

### Active Connections
```
GET /api/connections
```
Returns current connection counts by type.

### Configuration
```
GET /api/config
```
Returns current server configuration (if enabled).

### Ping
```
GET /api/ping
```
Simple connectivity test endpoint.

## WebSocket Protocol

The server maintains compatibility with the existing Pixel Streaming signalling protocol:

### Message Types

- **Connection Lifecycle**: `config`, `identify`, `disconnect`, `ping`, `pong`
- **WebRTC Signalling**: `offer`, `answer`, `iceCandidate`, `iceCandidateError`
- **Player Management**: `playerCount`, `playerConnected`, `playerDisconnected`
- **Streamer Management**: `streamerIdChanged`, `streamerDataChannels`, `streamerDisconnected`
- **SFU Support**: `sfuRecvDataChannelReady`, `sfuPeerDataChannelsReady`, `layerPreference`
- **Data Channels**: `dataChannelRequest`, `dataChannelOpen`, `dataChannelClose`
- **Error Handling**: `error`, `warning`

### Connection Flow

1. **Player Connection**: Connect to player port → Send `identify` → Receive `config` → Get subscribed to available streamer
2. **Streamer Connection**: Connect to streamer port → Send `identify` → Receive `config` → Start accepting player subscriptions
3. **SFU Connection**: Connect to SFU port → Send `identify` → Receive `config` → Handle multi-participant scenarios

## Architecture

### Core Components

- **SignallingApplication**: Main Spring Boot application class
- **SignallingServerOrchestrator**: Manages lifecycle of all WebSocket servers
- **ConnectionManager**: Handles connection lifecycle and routing
- **NettyWebSocketServer**: High-performance WebSocket server implementation
- **MessageHelper**: JSON message processing and serialization
- **AbstractConnection**: Base class for all connection types

### Connection Types

- **PlayerConnection**: Handles player WebSocket connections and message routing
- **StreamerConnection**: Manages streamer connections and subscriber relationships
- **SFUConnection**: Supports SFU-based multi-participant streaming

### Security

- **SecurityConfig**: Spring Security configuration for API endpoints
- **CORS Support**: Configurable cross-origin resource sharing
- **Rate Limiting**: Protection against connection flooding
- **Authentication**: Optional token-based authentication

## Monitoring and Metrics

The server exports Prometheus metrics including:

- Connection counts by type
- Message throughput
- Connection duration
- Error rates
- WebSocket frame statistics

Access metrics at: `http://localhost:8080/actuator/prometheus`

## Logging

Structured logging is configured with multiple outputs:

- **Console**: Human-readable format for development
- **File**: Rotating log files with retention
- **JSON**: Structured logs for log aggregation systems

Log files are written to the `logs/` directory:
- `signalling.log`: Standard text format
- `signalling.json`: JSON structured format

## Development

### Building from Source

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn clean package

# Run with development profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Testing

The project includes comprehensive unit tests for core components:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MessageHelperTest

# Run with coverage
mvn test jacoco:report
```

## Docker Support

### Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

COPY target/signalling-0.1.2.jar app.jar

EXPOSE 8080 8888 8889 8890

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Docker Compose

```yaml
version: '3.8'
services:
  signalling:
    build: .
    ports:
      - "8080:8080"   # HTTP API
      - "8888:8888"   # Streamer WebSocket
      - "8889:8889"   # Player WebSocket
      - "8890:8890"   # SFU WebSocket
    environment:
      - SIGNALLING_LOG_LEVEL=INFO
      - SIGNALLING_ENABLE_CORS=true
    volumes:
      - ./logs:/app/logs
```

## Performance

The Java implementation is designed for high performance:

- **Concurrent Connections**: Supports 1000+ concurrent WebSocket connections
- **Low Latency**: Netty-based event loop for minimal latency
- **Memory Efficient**: Optimized object allocation and garbage collection
- **Scalable**: Thread-safe connection management and message routing

### Performance Tuning

For high-load scenarios, consider:

- Increasing JVM heap size: `-Xmx2g`
- Tuning Netty worker threads
- Adjusting connection timeout settings
- Enabling JVM performance flags

## Migration from TypeScript

This Java implementation maintains full protocol compatibility with the existing TypeScript version:

- **Same Message Format**: JSON messages use identical structure
- **Same WebSocket Endpoints**: Port assignments and connection flow unchanged
- **Same REST API**: HTTP endpoints provide equivalent functionality
- **Configuration Mapping**: YAML configuration maps to TypeScript options

### Key Differences

1. **Performance**: Higher throughput and lower resource usage
2. **Type Safety**: Compile-time type checking and validation
3. **Enterprise Features**: Built-in monitoring, security, and configuration management
4. **JVM Ecosystem**: Access to mature Java libraries and tools

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Run the test suite
5. Submit a pull request

## License

Copyright Epic Games, Inc. All Rights Reserved.

This project is licensed under the MIT License - see the LICENSE file for details.