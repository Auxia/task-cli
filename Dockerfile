FROM eclipse-temurin:25-jre

WORKDIR /app
COPY target/task-cli-1.0.0-shaded.jar /app/task-cli.jar

ENTRYPOINT ["java","-jar","/app/task-cli.jar"]
