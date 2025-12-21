package example.bank.controller;

import example.bank.Notification;
import example.bank.model.Account;
import example.bank.model.OutboxEvent;
import example.bank.service.AccountService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import example.bank.repository.AccountRepository;
import example.bank.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    private final AccountRepository repository;
    private final AccountService service;
    private final OutboxEventRepository outboxRepository;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    private final KafkaJsonLogger kafkaJsonLogger;

    private Mono<String> currentLogin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> auth != null ? auth.getName() : "unknown")
                .defaultIfEmpty("unknown");
    }

    private void inc(String metric, String result, String login, String accountId) {
        Counter.builder(metric)
                .tag("result", result)
                .tag("login", login)
                .tag("account_id", accountId)
                .register(meterRegistry)
                .increment();
    }

    @PostMapping("/create")
    @Transactional
    public Mono<Account> createAccount(@RequestBody Account account) {
        if (account.getBalance() == null)
            account.setBalance(BigDecimal.ZERO);
        if (account.getOwnerId() == null)
            account.setOwnerId(account.getUsername());

        // обычный лог в консоль — ок
        log.info("Создание аккаунта: {}", account);

        return currentLogin()
                .flatMap(login -> repository.save(account)
                        .flatMap(savedAccount -> createOutboxEvent(savedAccount)
                                .then(Mono.fromRunnable(() -> kafkaJsonLogger.info(
                                        "account.create",
                                        login,
                                        String.valueOf(savedAccount.getId()),
                                        Map.of(
                                                "currency", savedAccount.getCurrency(),
                                                "balance", savedAccount.getBalance(),
                                                "username", savedAccount.getUsername(),
                                                "result", "success"))))
                                .thenReturn(savedAccount))
                        .doOnError(ex -> kafkaJsonLogger.error(
                                "account.create",
                                login,
                                null,
                                ex,
                                Map.of("result", "failure"))));
    }

    private Mono<OutboxEvent> createOutboxEvent(Account account) {
        Notification notification = buildNotification(account);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(notification);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        OutboxEvent event = new OutboxEvent(
                null,
                "ACCOUNT",
                String.valueOf(account.getId()),
                "ACCOUNT_CREATED",
                payloadJson,
                Instant.now(),
                false);

        return outboxRepository.save(event);
    }

    private Notification buildNotification(Account account) {
        String message = String.format(
                "Создан счёт №%d%nВалюта: %s%nВладелец: %s",
                account.getId(),
                account.getCurrency() != null ? account.getCurrency() : "не указана",
                account.getUsername());

        return new Notification(
                "account_created",
                account.getUsername(),
                null,
                null,
                message,
                account.getBalance(),
                account.getId());
    }

    @GetMapping
    public Flux<Account> getAccounts(@RequestParam(required = false) String username) {
        if (username != null) {
            log.info("Запрос счетов пользователя: {}", username);

            return currentLogin()
                    .doOnNext(login -> kafkaJsonLogger.info(
                            "account.list",
                            login,
                            null,
                            Map.of("username", username)))
                    .thenMany(repository.findByUsername(username));
        } else {
            log.info("Запрос всех счетов");
            return Flux.empty();
        }
    }

    @PostMapping("/{id}/deposit")
    public Mono<Void> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        log.info("Deposit request: id={}, amount={}", id, amount);

        return currentLogin()
                .flatMap(login -> service.deposit(id, amount)
                        .doOnSuccess(v -> {
                            inc("account_deposit_total", "success", login, String.valueOf(id));
                            kafkaJsonLogger.info(
                                    "account.deposit",
                                    login,
                                    String.valueOf(id),
                                    Map.of(
                                            "amount", amount,
                                            "result", "success"));
                        })
                        .doOnError(ex -> {
                            inc("account_deposit_total", "failure", login, String.valueOf(id));
                            kafkaJsonLogger.error(
                                    "account.deposit",
                                    login,
                                    String.valueOf(id),
                                    ex,
                                    Map.of(
                                            "amount", amount,
                                            "result", "failure"));
                        }));
    }

    @PostMapping("/{id}/withdraw")
    public Mono<Void> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        log.info("Withdraw request: id={}, amount={}", id, amount);

        return currentLogin()
                .flatMap(login -> service.withdraw(id, amount)
                        .doOnSuccess(v -> {
                            inc("account_withdraw_total", "success", login, String.valueOf(id));
                            kafkaJsonLogger.info(
                                    "account.withdraw",
                                    login,
                                    String.valueOf(id),
                                    Map.of(
                                            "amount", amount,
                                            "result", "success"));
                        })
                        .doOnError(ex -> {
                            inc("account_withdraw_total", "failure", login, String.valueOf(id));
                            kafkaJsonLogger.error(
                                    "account.withdraw",
                                    login,
                                    String.valueOf(id),
                                    ex,
                                    Map.of(
                                            "amount", amount,
                                            "result", "failure"));
                        }));
    }
}