FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN mvn -B dependency:go-offline

COPY src src
RUN mvn -B clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN mkdir -p /app/logs

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
