# Base image digests recorded 2026-09-22; update deliberately with the release gate.
FROM maven:3.9-eclipse-temurin-21@sha256:82e2ff483b3d5a95c351379f50aff9b50263579117c2b1c090dc7cfcce6216fb AS build
WORKDIR /build
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
COPY src ./src
COPY docker/HealthCheck.java ./docker/HealthCheck.java
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package && javac -d /health docker/HealthCheck.java

FROM eclipse-temurin:21-jre@sha256:6cbdfc89c9657478bc5abea638030310f6c0267404e98a5808097bb1925932f1
RUN groupadd --gid 10001 cinevora && useradd --uid 10001 --gid 10001 --no-create-home cinevora
WORKDIR /app
COPY --from=build /build/target/cinevora-backend-0.1.0-SNAPSHOT.jar /app/app.jar
COPY --from=build /health /opt/health
RUN mkdir -p /app/uploads/media && chown -R 10001:10001 /app/uploads
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod PORT=8080 JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
STOPSIGNAL SIGTERM
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 CMD ["java", "-cp", "/opt/health", "HealthCheck"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
