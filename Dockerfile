FROM openjdk:11.0.3-jre-stretch
EXPOSE 8080
ADD github-initializer-1.0-SNAPSHOT.jar github-initializer.jar
ENTRYPOINT ["java", "-jar", "github-initializer.jar"]