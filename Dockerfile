FROM sourcemation/jdk-21:jdk-21.0.9-10
LABEL authors="kemalthes"
WORKDIR /app
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]