FROM arm64v8/openjdk:19
### Use the line above if you're on an M1
### Use the line below if you're on an amd64 macahine
# FROM openjdk:19-jdk-alpine
MAINTAINER Jordan Thevenow-Harrison <jtth@jtth.net>

ADD target/webdir-api-standalone.jar /webdir-api/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/webdir-api/app.jar"]