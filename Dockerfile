# Stage 1: Build
FROM maven:3.8-openjdk-11 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/car-pooling-1.0.0-SNAPSHOT.jar /app/car-pooling.jar

# Expose port 9091 (as defined in application.properties)
EXPOSE 9091

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:9091/status || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/car-pooling.jar"]
