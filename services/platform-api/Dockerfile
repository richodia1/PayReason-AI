FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY services/platform-api services/platform-api
RUN mvn -B -pl services/platform-api -am package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/platform-api/target/platform-api-0.1.0-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8180
ENTRYPOINT ["java", "-jar", "app.jar"]
