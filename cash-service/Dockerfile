# ========= Этап 1: сборка приложения =========
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Подключаем зеркало для стабильных загрузок
COPY /settings.xml /root/.m2/settings.xml

# 1. Собираем bank-events
COPY bank-events/pom.xml ./bank-events/pom.xml
COPY bank-events/src ./bank-events/src
RUN mvn -f bank-events/pom.xml clean install -DskipTests

# # Копируем кастомный pom и исходники
COPY cash-service/pom-docker.xml ./pom.xml
COPY cash-service/src ./src

# Скачиваем зависимости и собираем
# RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests

# Этап 2: минимальный runtime-образ
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем только готовый JAR
COPY --from=builder /app/target/cash-service-*.jar app.jar

# Открываем порт, указанный в application.yml
EXPOSE 8083

# Указываем стандартную команду запуска Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]

