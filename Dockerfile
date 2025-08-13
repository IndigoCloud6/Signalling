# Multi-stage build for efficient Docker image
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Add non-root user for security
RUN addgroup -g 1001 -S signalling && \
    adduser -u 1001 -S signalling -G signalling

# Set working directory
WORKDIR /app

# Create logs directory
RUN mkdir -p logs && chown signalling:signalling logs

# Copy built JAR from builder stage
COPY --from=builder /app/target/signalling-*.jar app.jar

# Change ownership to non-root user
RUN chown signalling:signalling app.jar

# Switch to non-root user
USER signalling

# Expose ports
EXPOSE 8080 8888 8889 8890

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]