# The build context is the repository root (see docker-compose.yml and
# render.yaml), so every COPY path is relative to the repository root.
# The root .dockerignore keeps the ignored local application.properties and any
# .env files out of the context, so local credentials are never baked in.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY backend/.mvn ./.mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src ./src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as an unprivileged account; the image only needs read access to the jar.
RUN groupadd --system --gid 10001 campuscore \
    && useradd --system --uid 10001 --gid campuscore --home-dir /app --shell /usr/sbin/nologin campuscore

# Copy the packaged application regardless of the project version.
COPY --from=build --chown=campuscore:campuscore /workspace/target/*.jar /app/app.jar

USER campuscore
EXPOSE 8080

# Size the heap from the container memory limit instead of host memory.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]