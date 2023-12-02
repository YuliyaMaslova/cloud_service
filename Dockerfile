FROM openjdk:17

EXPOSE 8080

ADD target/cloud_service-0.0.1-SNAPSHOT.jar backend.jar

CMD ["java", "-jar", "backend.jar"]
