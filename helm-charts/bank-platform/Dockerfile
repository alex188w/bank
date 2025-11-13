# ===== Build stage =====
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Аргументы, передаваемые из Jenkins
ARG SERVICE_NAME
ARG SERVICE_PORT=8080

# Настройка Maven-зеркала (если есть)
COPY settings.xml /root/.m2/settings.xml

# Копируем исходники конкретного микросервиса
COPY ${SERVICE_NAME}/pom-docker.xml ./pom.xml
COPY ${SERVICE_NAME}/src ./src

# Сборка
RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app

ARG SERVICE_NAME
ARG SERVICE_PORT=8080
COPY --from=builder /app/target/${SERVICE_NAME}-*.jar app.jar

# Экспортируем порт, но фиксируем не жёстко 
EXPOSE ${SERVICE_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]