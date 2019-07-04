FROM openjdk:11.0.3-jre-stretch
ENV PORT=8080
EXPOSE 8080
ADD github-initializer-1.0-SNAPSHOT.jar github-initializer.jar
CMD [ "java", "-XX:+UseContainerSupport", "-Dserver.port=${PORT}", "-jar", "github-initializer.jar" ]