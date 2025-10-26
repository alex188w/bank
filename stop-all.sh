#!/bin/bash
# stop-all.sh — останавливает все микросервисы

# Файлы для хранения PID каждого сервиса
SERVICES=("gateway-service" "account-service" "cash-service" "exchange-generator" "exchange-service" "transfer-service" "notification-service")

for SERVICE in "${SERVICES[@]}"; do
    PID_FILE="/tmp/${SERVICE}.pid"
    if [[ -f "$PID_FILE" ]]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "Останавливаем $SERVICE (PID=$PID)..."
            kill $PID
            # Проверка, завершился ли процесс
            sleep 1
            if ps -p $PID > /dev/null 2>&1; then
                echo "PID=$PID не завершился, принудительно..."
                kill -9 $PID
            fi
        else
            echo "$SERVICE уже остановлен"
        fi
        rm -f "$PID_FILE"
    else
        echo "PID файл для $SERVICE не найден, пропускаем"
    fi
done

echo "Все сервисы остановлены."