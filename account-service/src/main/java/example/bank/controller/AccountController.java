package example.bank.controller;

import example.bank.model.Account;
import example.bank.service.AccountService;
import example.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

    private final AccountRepository repository;
    private final AccountService service;

    @GetMapping
    public Flux<Account> all() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<Account> getById(@PathVariable Long id) {
        return repository.findById(id);
    }

    @PostMapping
    public Mono<Account> create(@RequestBody Account account) {
        return repository.save(account);
    }

    @PostMapping("/{id}/deposit")
    public Mono<Account> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return service.deposit(id, amount);
    }

    @PostMapping("/{id}/withdraw")
    public Mono<Account> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return service.withdraw(id, amount);
    }
}
