package example.bank.controller;

import example.bank.model.Account;
import example.bank.repository.AccountRepository;
import example.bank.service.AccountService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    private final AccountRepository repository;
    private final AccountService service;
    private final MeterRegistry meterRegistry;
    private final AccountObservability audit;

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

    @GetMapping
    public Flux<Account> getAccounts(ServerWebExchange exchange,
            @RequestParam(required = false) String username) {
        if (username == null) {
            log.info("Запрос всех счетов");
            return Flux.empty();
        }

        log.info("Запрос счетов пользователя: {}", username);

        return currentLogin()
                .doOnNext(login -> audit.accountList(exchange, login, username))
                .thenMany(repository.findByUsername(username))
                .doOnError(ex -> audit.accountListFailed(exchange, username, ex));
    }

    @GetMapping("/{id}")
    public Mono<Account> getById(@PathVariable Long id) {
        return repository.findById(id);
    }

    @PostMapping("/{id}/deposit")
    public Mono<Void> deposit(ServerWebExchange exchange,
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        log.info("Deposit request: id={}, amount={}", id, amount);

        return currentLogin()
                .flatMap(login -> service.deposit(id, amount)
                        .doOnSuccess(v -> {
                            inc("account_deposit_total", "success", login, String.valueOf(id));
                            audit.depositSuccess(exchange, login, String.valueOf(id));
                        })
                        .doOnError(ex -> {
                            inc("account_deposit_total", "failure", login, String.valueOf(id));
                            audit.depositFailed(exchange, login, String.valueOf(id), ex);
                        }));
    }

    @PostMapping("/{id}/withdraw")
    public Mono<Void> withdraw(ServerWebExchange exchange,
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        log.info("Withdraw request: id={}, amount={}", id, amount);

        return currentLogin()
                .flatMap(login -> service.withdraw(id, amount)
                        .doOnSuccess(v -> {
                            inc("account_withdraw_total", "success", login, String.valueOf(id));
                            audit.withdrawSuccess(exchange, login, String.valueOf(id));
                        })
                        .doOnError(ex -> {
                            inc("account_withdraw_total", "failure", login, String.valueOf(id));
                            audit.withdrawFailed(exchange, login, String.valueOf(id), ex);
                        }));
    }
}