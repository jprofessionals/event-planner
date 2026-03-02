FROM ghcr.io/graalvm/native-image-community:21 AS build
RUN microdnf install -y findutils && microdnf clean all
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false --no-daemon -x test

FROM debian:bookworm-slim
RUN useradd -r -u 1001 appuser
WORKDIR /app
COPY --from=build /app/build/*-runner /app/application
RUN chmod +x /app/application
USER appuser
EXPOSE 8080
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
