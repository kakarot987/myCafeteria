FROM maven:3.8.8-eclipse-temurin-8 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=builder /app/target/user-mysql.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
