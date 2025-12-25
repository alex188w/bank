package example.bank.controller;

import java.util.Map;

import example.bank.logging.KafkaJsonLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

@Service
@RequiredArgsConstructor
public class CashObservability {

    private final KafkaJsonLogger kafka;

    public void depositRequested(ServerWebExchange ex, String login, Long accountId) {
        kafka.info(ex, "cash.deposit", "Cash deposit requested",
                Map.of("login", login, "accountId", String.valueOf(accountId)));
    }

    public void depositSuccess(ServerWebExchange ex, String login, Long accountId) {
        kafka.info(ex, "cash.deposit", "Cash deposit success",
                Map.of("login", login, "accountId", String.valueOf(accountId), "result", "success"));
    }

    public void depositFailed(ServerWebExchange ex, String login, Long accountId, Throwable err) {
        kafka.error(ex, "cash.deposit", "Cash deposit failed", err,
                Map.of("login", login, "accountId", String.valueOf(accountId), "result", "failure"));
    }

    public void withdrawRequested(ServerWebExchange ex, String login, Long accountId) {
        kafka.info(ex, "cash.withdraw", "Cash withdraw requested",
                Map.of("login", login, "accountId", String.valueOf(accountId)));
    }

    public void withdrawSuccess(ServerWebExchange ex, String login, Long accountId) {
        kafka.info(ex, "cash.withdraw", "Cash withdraw success",
                Map.of("login", login, "accountId", String.valueOf(accountId), "result", "success"));
    }

    public void withdrawFailed(ServerWebExchange ex, String login, Long accountId, Throwable err) {
        kafka.error(ex, "cash.withdraw", "Cash withdraw failed", err,
                Map.of("login", login, "accountId", String.valueOf(accountId), "result", "failure"));
    }

    public void notificationSent(ServerWebExchange ex, String login, Long accountId, String type) {
        kafka.info(ex, "cash.notification", "Notification sent",
                Map.of("login", login, "accountId", String.valueOf(accountId), "type", type, "result", "success"));
    }

    public void notificationFailed(ServerWebExchange ex, String login, Long accountId, String type, Throwable err) {
        kafka.error(ex, "cash.notification", "Notification send failed", err,
                Map.of("login", login, "accountId", String.valueOf(accountId), "type", type, "result", "failure"));
    }
}
