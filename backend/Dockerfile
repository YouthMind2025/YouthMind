# Multi-stage build for YouthMind Backend
FROM maven:3.9.6-openjdk-17-slim AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r youthmind && useradd -r -g youthmind youthmind

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/youthmind-backend-*.jar app.jar

# Change ownership to app user
RUN chown youthmind:youthmind app.jar

# Switch to app user
USER youthmind

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
