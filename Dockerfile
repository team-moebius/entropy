# Build
FROM openjdk:14-jdk-slim as build
COPY /build.gradle settings.gradle gradlew ./
COPY /gradle gradle
COPY src src
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar

# Run
FROM openjdk:14-jdk-slim
ENV ARTIFACT_PATH=build/libs/*.jar
COPY --from=build $ARTIFACT_PATH app.jar
ENTRYPOINT ["java","-Dspring.profiles.active=prod","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005","-jar","app.jar"]
