package example.bank.logging;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import org.springframework.web.server.ServerWebExchange;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaJsonLogger {

    private final @Qualifier("logKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaLogProperties props;

    public void info(ServerWebExchange exchange, String action, String msg, Map<String, ?> fields) {
        send(exchange, "INFO", action, msg, fields, null);
    }

    public void warn(ServerWebExchange exchange, String action, String msg, Map<String, ?> fields) {
        send(exchange, "WARN", action, msg, fields, null);
    }

    public void debug(ServerWebExchange exchange, String action, String msg, Map<String, ?> fields) {
        send(exchange, "DEBUG", action, msg, fields, null);
    }

    public void error(ServerWebExchange exchange, String action, String msg, Throwable ex, Map<String, ?> fields) {
        send(exchange, "ERROR", action, msg, fields, ex);
    }

    private void send(ServerWebExchange exchange,
                      String level,
                      String action,
                      String msg,
                      Map<String, ?> fields,
                      Throwable ex) {

        if (props == null || !props.enabled()) return;

        try {
            ObjectNode root = objectMapper.createObjectNode();

            root.put("ts", Instant.now().toString());
            root.put("level", level);
            root.put("service", serviceSafe());
            root.put("env", envSafe());

            root.putObject("event").put("action", action);
            root.put("message", msg);

            // trace/span из exchange attributes (W3C traceparent распарсен фильтром)
            if (exchange != null) {
                Object t = exchange.getAttributes().get(TraceContextFilter.ATTR_TRACE_ID);
                Object s = exchange.getAttributes().get(TraceContextFilter.ATTR_SPAN_ID);
                if (t instanceof String tt && !tt.isBlank()) root.put("trace.id", tt);
                if (s instanceof String ss && !ss.isBlank()) root.put("span.id", ss);
            }

            if (fields != null) {
                fields.forEach((k, v) -> {
                    if (v == null) return;
                    String key = k.toLowerCase();
                    if (key.contains("password") || key.contains("token") || key.contains("secret")) return;
                    if ("amount".equalsIgnoreCase(k)) return; // политика
                    root.set(k, objectMapper.valueToTree(v));
                });
            }

            if (ex != null) {
                root.put("error.type", ex.getClass().getName());
                root.put("error.message", ex.getMessage() == null ? "" : ex.getMessage());
            }

            String json = objectMapper.writeValueAsString(root);

            // key для партиционирования
            kafkaTemplate.send(topicSafe(), serviceSafe(), json);

        } catch (Exception e) {
            log.warn("Kafka log build/send error action={}", action, e);
        }
    }

    private String topicSafe() {
        return (props.topic() == null || props.topic().isBlank()) ? "bank-platform-logs" : props.topic();
    }

    private String envSafe() {
        return (props.env() == null || props.env().isBlank()) ? "dev" : props.env();
    }

    private String serviceSafe() {
        return (props.service() == null || props.service().isBlank()) ? "cash-service" : props.service();
    }
}