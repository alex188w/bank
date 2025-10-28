# Этап 1: сборка приложения
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Этап 2: минимальный runtime-образ
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем только готовый JAR
COPY --from=builder /app/target/transfer-service-*.jar app.jar

# Открываем порт, указанный в application.yml
EXPOSE 8086

# Указываем стандартную команду запуска Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]