FROM adoptopenjdk/openjdk11:alpine-jre
WORKDIR /usr/src/app

COPY target/rabbitConsumer-1.0-SNAPSHOT.jar .

CMD ["java", "-jar", "rabbitConsumer-1.0-SNAPSHOT.jar"]