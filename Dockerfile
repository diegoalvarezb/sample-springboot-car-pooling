##########################################
# DEV: Maven + JDK (live reload / debug) #
##########################################
FROM maven:3.9-eclipse-temurin-11 AS dev

WORKDIR /app

# Cache dependencies (stable layer)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy source code (so it works even without volumes)
COPY src ./src

# Expose ports: app and debug
EXPOSE 9091 5005

# Default: dev mode (you can override CMD with docker run if needed)
CMD ["mvn", "spring-boot:run"]



##############################
# BUILD: builds the JAR file #
##############################
FROM maven:3.9-eclipse-temurin-11 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src

# Build (skipping tests as before; change if you want to run them)
RUN mvn -B -q clean package -DskipTests



######################
# PROD: runtime only #
######################
FROM eclipse-temurin:11-jre AS prod

WORKDIR /app

# Copy only the final artifact
COPY --from=build /app/target/car-pooling-1.0.0-SNAPSHOT.jar /app/car-pooling.jar

EXPOSE 9091

CMD ["java", "-jar", "/app/car-pooling.jar"]
