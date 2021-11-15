FROM openjdk:8
VOLUME /tmp
ADD target/springboot-docker-compose-mysql.jar springboot-docker-compose-mysql.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","springboot-docker-compose-mysql.jar"]