# --- Stage 1: Build Environment ---
FROM maven:3.8.8-eclipse-temurin-17 AS builder
WORKDIR /app

# Optimize build execution time using Docker layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package artifact
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Minimal Production Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user for security compliance
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built executable jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose standard port container metrics (override via environment variables if needed)
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urand", "-jar", "app.jar"]