# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy common/sns-lib first and publish to Maven local
COPY common/sns-lib common/sns-lib
WORKDIR /app/common/sns-lib
# Ensure settings.gradle exists (it should be copied with the directory)
RUN gradle publishToMavenLocal --no-daemon

# Build service-ticket
WORKDIR /app
COPY build.gradle ./
COPY gradle gradle
COPY src src
# Docker 빌드 시에는 Maven local 의존성 사용 (settings.gradle에서 common:sns-lib 제외)
RUN echo "rootProject.name = 'ticketservice-service'" > settings.gradle
RUN gradle build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
