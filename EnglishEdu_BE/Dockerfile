# ─────────────────────────────────────────────
# Stage 1: Build Spring Boot JAR
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper + POM first (layer cache for dependencies)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Fix Windows CRLF line endings in mvnw
RUN sed -i 's/\r//' mvnw && chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2: Minimal runtime image
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 4000

ENTRYPOINT ["java", "-jar", "app.jar"]
