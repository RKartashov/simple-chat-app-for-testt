FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system chat && useradd --system --gid chat chat
COPY --from=build /app/build/libs/*.jar /app/app.jar
RUN chown chat:chat /app/app.jar
USER chat

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
