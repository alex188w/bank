FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/gateway-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]