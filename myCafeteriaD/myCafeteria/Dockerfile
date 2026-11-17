FROM maven:3.8.8-eclipse-temurin-11 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:11-jre
WORKDIR /app
RUN useradd --system --uid 10001 cafeteria
COPY --from=build /workspace/target/myCafeteria-0.0.1-SNAPSHOT.jar app.jar
USER cafeteria
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
