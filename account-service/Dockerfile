# ========= Этап 1: сборка приложения =========
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Подключаем зеркало для стабильных загрузок
COPY /settings.xml /root/.m2/settings.xml

# --- 1. Сначала собираем bank-events как библиотеку ---

# Копируем POM и исходники bank-events
COPY bank-events/pom.xml ./bank-events/pom.xml
COPY bank-events/src ./bank-events/src

# Собираем и устанавливаем в локальный репозиторий контейнера (~/.m2)
RUN mvn -f bank-events/pom.xml clean install -DskipTests

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