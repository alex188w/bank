#!/bin/bash
# mvn -pl auth-service spring-boot:run & echo $! > /tmp/auth-service.pid
mvn -pl gateway-service spring-boot:run & echo $! > /tmp/gateway-service.pid
mvn -pl account-service spring-boot:run & echo $! > /tmp/account-service.pid
mvn -pl cash-service spring-boot:run & echo $! > /tmp/cash-service.pid
mvn -pl exchange-generator spring-boot:run & echo $! > /tmp/exchange-generator.pid
mvn -pl exchange-service spring-boot:run & echo $! > /tmp/exchange-service.pid
wait