package example.bank.controller;

import java.util.Map;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AccountObservability {

    private final KafkaJsonLogger kafka;

    public void accountList(ServerWebExchange exchange, String login, String username) {
        kafka.info(exchange, "account.list", "Account list requested",
                Map.of("login", login, "username", username));
    }

    public void accountListFailed(ServerWebExchange exchange, String username, Throwable ex) {
        kafka.error(exchange, "account.list", "Account list failed", ex,
                Map.of("username", username));
    }

    public void depositSuccess(ServerWebExchange exchange, String login, String accountId) {
        kafka.info(exchange, "account.deposit", "Deposit success",
                Map.of("login", login, "accountId", accountId, "result", "success"));
    }

    public void depositFailed(ServerWebExchange exchange, String login, String accountId, Throwable ex) {
        kafka.error(exchange, "account.deposit", "Deposit failed", ex,
                Map.of("login", login, "accountId", accountId, "result", "failure"));
    }

    public void withdrawSuccess(ServerWebExchange exchange, String login, String accountId) {
        kafka.info(exchange, "account.withdraw", "Withdraw success",
                Map.of("login", login, "accountId", accountId, "result", "success"));
    }

    public void withdrawFailed(ServerWebExchange exchange, String login, String accountId, Throwable ex) {
        kafka.error(exchange, "account.withdraw", "Withdraw failed", ex,
                Map.of("login", login, "accountId", accountId, "result", "failure"));
    }
}
