FROM gradle:jdk10 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle clean build -x test

FROM openjdk:10-jre-slim

EXPOSE 8080
EXPOSE 8081

COPY --from=builder /home/gradle/src/build/libs/kateway-0.0.1.jar /app/
WORKDIR /app

ENTRYPOINT java -jar /app/kateway-0.0.1.jar