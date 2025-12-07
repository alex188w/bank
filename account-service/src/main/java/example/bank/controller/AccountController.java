package example.bank.controller;

import example.bank.Notification;
import example.bank.model.Account;
import example.bank.service.AccountService;
import example.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
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

    private final KafkaTemplate<String, Notification> kafkaTemplate;

    @Value("${app.kafka.topics.notifications}")
    private String notificationsTopic;

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

    @PostMapping("/create")
    public Mono<Account> createAccount(@RequestBody Account account) {
        if (account.getBalance() == null)
            account.setBalance(BigDecimal.ZERO);
        if (account.getOwnerId() == null)
            account.setOwnerId(account.getUsername());

        log.info("Создание аккаунта: {}", account);

        return repository.save(account)
                .flatMap(savedAccount -> sendNotification(savedAccount)
                        .thenReturn(savedAccount));
    }

    private Mono<Void> sendNotification(Account account) {

        String message = String.format(
                "Создан счёт №%d%nВалюта: %s%nВладелец: %s",
                account.getId(),
                account.getCurrency() != null ? account.getCurrency() : "не указана",
                account.getUsername());

        Notification notification = new Notification(
                "account_created",
                account.getUsername(),
                null,
                null,
                message,
                account.getBalance(),
                account.getId());

        return Mono.fromFuture(
                kafkaTemplate.send(
                        notificationsTopic,
                        String.valueOf(account.getId()),
                        notification))
                .doOnSuccess(result -> log.info("Отправлено уведомление в Kafka: topic={}, partition={}, offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()))
                .doOnError(e -> log.error("Ошибка при отправке уведомления в Kafka", e))
                .then();
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
