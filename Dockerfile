# Multi-stage Dockerfile: build stage and runtime stage
FROM eclipse-temurin:11-jdk AS build

WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:11-jdk

WORKDIR /app

# Install Maven and curl (Maven needed for dev-live and dev-debug)
RUN apt-get update && \
    apt-get install -y maven curl && \
    rm -rf /var/lib/apt/lists/*

# Copy the JAR from build stage
COPY --from=build /app/target/car-pooling-1.0.0-SNAPSHOT.jar /app/car-pooling.jar

# Expose ports: 9091 for app, 5005 for debugging
EXPOSE 9091 5005

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:9091/status || exit 1

# Default: run JAR (production mode)
# For development with live reload, override CMD in docker run
CMD ["java", "-jar", "/app/car-pooling.jar"]
