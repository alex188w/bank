package example.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaJsonLogger {

  private final @Qualifier("logKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.logs.kafka.topic:bank-platform-logs}")
  private String topic;

  @Value("${ENV:dev}")
  private String env;

  @Value("${spring.application.name:account-service}")
  private String service;

  public void info(String event, String login, String accountId, Map<String, Object> fields) {
    publish("INFO", event, login, accountId, fields, null);
  }

  public void error(String event, String login, String accountId, Throwable ex, Map<String, Object> fields) {
    publish("ERROR", event, login, accountId, fields, ex);
  }

  private void publish(String level, String event, String login, String accountId,
      Map<String, Object> fields, Throwable ex) {
    try {
      var payload = new LinkedHashMap<String, Object>();
      payload.put("ts", Instant.now().toString());
      payload.put("level", level);
      payload.put("service", service);
      payload.put("env", env);
      payload.put("event", event);
      if (login != null)
        payload.put("login", login);
      if (accountId != null)
        payload.put("accountId", accountId);
      if (fields != null)
        payload.putAll(fields);
      if (ex != null) {
        payload.put("exception", ex.getClass().getName());
        payload.put("error", ex.getMessage());
      }

      String json = objectMapper.writeValueAsString(payload);

      kafkaTemplate.send(topic, service, json)
          .whenComplete((res, sendEx) -> {
            if (sendEx != null) {
              log.warn("Kafka log send failed topic={} service={} event={}", topic, service, event, sendEx);
            } else {
              log.debug("Kafka log sent topic={} partition={} offset={}",
                  topic, res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
            }
          });

    } catch (Exception e) {
      log.warn("Kafka log build/send error topic={} service={} event={}", topic, service, event, e);
    }
  }
}
