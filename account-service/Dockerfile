# ========= Этап 1: сборка приложения =========
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Подключаем зеркало для стабильных загрузок
COPY /settings.xml /root/.m2/settings.xml

# Копируем кастомный pom и исходники
COPY account-service/pom-docker.xml ./pom.xml
COPY account-service/src ./src

# Скачиваем зависимости и собираем
# RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests

# Этап 2: минимальный runtime-образ
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/account-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]