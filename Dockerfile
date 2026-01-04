FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="sanedge"
LABEL version="1.0.0"
LABEL description="Vert.x App with OpenTelemetry"

RUN addgroup -S app && adduser -S app -G app && \
  apk add --no-cache curl
WORKDIR /app

COPY --from=build /app/target/app.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8888

ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75 \
  -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom"

ENV OTEL_SERVICE_NAME=app
ENV OTEL_SERVICE_VERSION=1.0.0
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_RESOURCE_ATTRIBUTES=service.name=app,service.version=1.0.0,deployment.environment=docker

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
