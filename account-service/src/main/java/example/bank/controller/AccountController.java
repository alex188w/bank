package example.bank.controller;

import example.bank.Notification;
import example.bank.model.Account;
import example.bank.model.OutboxEvent;
import example.bank.service.AccountService;
import example.bank.repository.AccountRepository;
import example.bank.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.http.MediaType;
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

    private final ObjectMapper objectMapper; // для сериализации Notification в JSON

    @PostMapping("/create")
    @Transactional
    public Mono<Account> createAccount(@RequestBody Account account) {
        if (account.getBalance() == null)
            account.setBalance(BigDecimal.ZERO);
        if (account.getOwnerId() == null)
            account.setOwnerId(account.getUsername());

        log.info("Создание аккаунта: {}", account);

        return repository.save(account)
                .flatMap(savedAccount -> createOutboxEvent(savedAccount)
                        .thenReturn(savedAccount));
    }

    private Mono<OutboxEvent> createOutboxEvent(Account account) {
        Notification notification = buildNotification(account);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(notification);
        } catch (JsonProcessingException e) {
            // Если не можем сериализовать — лучше вообще откатить транзакцию
            return Mono.error(e);
        }

        OutboxEvent event = new OutboxEvent(
                null,
                "ACCOUNT",
                String.valueOf(account.getId()),
                "ACCOUNT_CREATED",
                payloadJson,
                Instant.now(),
                false
        );

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
            return repository.findByUsername(username);
        } else {
            log.info("Запрос всех счетов");
            return Flux.empty();
        }
    }

    @GetMapping("/{id}")
    public Mono<Account> getById(@PathVariable Long id) {
        return repository.findById(id);
    }

    @PostMapping("/{id}/deposit")
    public Mono<Void> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        log.info("Deposit request: id={}, amount={}", id, amount);
        return service.deposit(id, amount);
    }

    @PostMapping("/{id}/withdraw")
    public Mono<Void> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        log.info("Withdraw request: id={}, amount={}", id, amount);
        return service.withdraw(id, amount);
    }
}