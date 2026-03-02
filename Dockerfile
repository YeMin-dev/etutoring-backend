# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=builder /build/target/*.jar /app/app.jar

ENV JAVA_OPTS=""
ENV APP_ARGS=""

EXPOSE 8080
USER spring:spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $APP_ARGS"]
