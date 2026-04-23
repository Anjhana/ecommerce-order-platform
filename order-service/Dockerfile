FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/order-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar",             "-Dspring.profiles.active=docker",             "-XX:+UseContainerSupport",             "-XX:MaxRAMPercentage=75.0",             "app.jar"]
