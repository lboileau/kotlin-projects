FROM node:22-slim AS webapp-build
WORKDIR /webapp
COPY webapp/package.json webapp/package-lock.json ./
RUN npm ci
COPY webapp/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
COPY libs libs
COPY clients clients
COPY services services
RUN ./gradlew :services:camper-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/camper-service/build/libs/*.jar app.jar
COPY --from=webapp-build /webapp/dist /app/static
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
