#!/bin/bash
mvn -pl gateway-service spring-boot:run & echo $! > /tmp/gateway-service.pid
mvn -pl account-service spring-boot:run & echo $! > /tmp/account-service.pid
mvn -pl cash-service spring-boot:run & echo $! > /tmp/cash-service.pid
mvn -pl exchange-generator spring-boot:run & echo $! > /tmp/exchange-generator.pid
mvn -pl exchange-service spring-boot:run & echo $! > /tmp/exchange-service.pid
mvn -pl transfer-service spring-boot:run & echo $! > /tmp/transfer-service.pid
mvn -pl notification-service spring-boot:run & echo $! > /tmp/notification-service.pid
wait