# Build Stage
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B || true

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Runtime Stage
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd -r saas && useradd -r -g saas saas

USER saas:saas

COPY --from=builder --chown=saas:saas /app/target/*.jar app.jar

EXPOSE 8090

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
