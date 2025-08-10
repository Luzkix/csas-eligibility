# 1) Build stage – Maven s JDK 21
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY src src
RUN ./mvnw clean package -DskipTests

# 2) Runtime stage – JRE 21
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar

ENV SERVER_PORT=8080
ENV LOGGING_LVL=WARN

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
