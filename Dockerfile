FROM openjdk:21
WORKDIR /app
COPY ./target/auth-service-1.0.0-SNAPSHOT.jar /app
EXPOSE 8085
CMD ["java", "-jar", "auth-service-1.0.0-SNAPSHOT.jar","--spring.profiles.active=docker"]