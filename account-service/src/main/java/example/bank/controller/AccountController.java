package example.bank.controller;

import example.bank.logging.AccountAudit;
import example.bank.model.Account;
import example.bank.repository.AccountRepository;
import example.bank.repository.OutboxEventRepository;
import example.bank.service.AccountService;
import example.bank.Notification;
import example.bank.model.OutboxEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

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

    private final AccountAudit audit;

    private Mono<String> currentLogin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> auth != null ? auth.getName() : "unknown")
                .defaultIfEmpty("unknown");
    }

    private void incBusiness(String type, String status) {
        Counter.builder("business_operation_total")
                .tag("service", "account")
                .tag("type", type) // deposit/withdraw/list/create
                .tag("status", status) // success/failure
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

        // обычный лог (консоль + kafka через appender)
        log.info("Account create request");

        return currentLogin()
                .flatMap(login -> repository.save(account)
                        .flatMap(saved -> createOutboxEvent(saved)
                                .doOnSuccess(v -> {
                                    incBusiness("create", "success");
                                    audit.info("account.create",
                                            "Account create succes=" + login + " accountId=" + saved.getId()
                                                    + " username=" + saved.getUsername(),
                                            "create", "success");
                                })
                                .thenReturn(saved))
                        .doOnError(ex -> {
                            incBusiness("create", "failure");
                            audit.error(
                                    "account.create",
                                    "Account create failed login=" + login,
                                    ex);
                        }));
    }

    @GetMapping
    public Flux<Account> getAccounts(@RequestParam(required = false) String username) {
        if (username == null) {
            log.info("Account list request: all");
            return Flux.empty();
        }

        log.info("Account list request");

        return currentLogin()
                .flatMapMany(login -> {
                    incBusiness("list", "success");
                    audit.info(
                            "account.list",
                            "Account list requested login=" + login +
                                    " username=" + username +
                                    " amount=\"\"",
                            "account.list", "success");
                    return repository.findByUsername(username);
                })
                .doOnError(ex -> {
                    incBusiness("list", "failure");
                    audit.error(
                            "account.list",
                            "Account list failed username=" + username +
                                    " amount=\"\"",
                            ex);
                });
    }

    @GetMapping("/{id}")
    public Mono<Account> getById(@PathVariable Long id) {
        return repository.findById(id);
    }

    @PostMapping("/{id}/deposit")
    public Mono<Void> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        // amount в обычном логе не показываем — но оставим признак что поле существует
        log.info("Deposit request: id={}, amount=\"\"", id);

        return currentLogin()
                .flatMap(login -> service.deposit(id, amount)
                        .doOnSuccess(v -> {
                            incBusiness("deposit", "success");
                            audit.info("account.deposit",
                                    "Deposit success login=" + login + " accountId=" + id + " amount=\"\"",
                                    "deposit", "success");
                        })
                        .doOnError(ex -> {
                            incBusiness("deposit", "failure");
                            audit.error(
                                    "account.deposit",
                                    "Deposit failed login=" + login +
                                            " accountId=" + id +
                                            " amount=\"\"",
                                    ex);
                        }));
    }

    @PostMapping("/{id}/withdraw")
    public Mono<Void> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        log.info("Withdraw request: id={}, amount=\"\"", id);

        return currentLogin()
                .flatMap(login -> service.withdraw(id, amount)
                        .doOnSuccess(v -> {
                            incBusiness("withdraw", "success");
                            audit.info("account.withdraw",
                                    "Withdraw success login=" + login + " accountId=" + id + " amount=\"\"",
                                    "withdraw", "success");
                        })
                        .doOnError(ex -> {
                            incBusiness("withdraw", "failure");
                            audit.error(
                                    "account.withdraw",
                                    "Withdraw failed login=" + login +
                                            " accountId=" + id +
                                            " amount=\"\"",
                                    ex);
                        }));
    }

    private Mono<OutboxEvent> createOutboxEvent(Account account) {
        Notification notification = buildNotification(account);

        final String payloadJson;
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
        String msg = String.format(
                "Создан счёт №%d%nВалюта: %s%nВладелец: %s",
                account.getId(),
                account.getCurrency() != null ? account.getCurrency() : "не указана",
                account.getUsername());

        return new Notification(
                "account_created",
                account.getUsername(),
                null,
                null,
                msg,
                account.getBalance(),
                account.getId());
    }
}