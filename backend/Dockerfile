# ============================================================
# Dockerfile - Clinica Stella Maris Backend
# Multi-stage build: compila con Maven, corre solo el JAR final
# ============================================================

# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-22 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime (imagen liviana, sin Maven ni codigo fuente)
FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=build /app/target/clinica-stella-maris-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
