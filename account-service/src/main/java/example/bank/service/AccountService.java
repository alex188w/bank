package example.bank.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import example.bank.model.Account;
import example.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository repository;

    public Mono<Account> deposit(Long id, BigDecimal amount) {
        return repository.findById(id)
                .flatMap(acc -> {
                    acc.setBalance(acc.getBalance().add(amount));
                    return repository.save(acc);
                });
    }

    public Mono<Account> withdraw(Long id, BigDecimal amount) {
        return repository.findById(id)
                .flatMap(acc -> {
                    if (acc.getBalance().compareTo(amount) < 0)
                        return Mono.error(new IllegalStateException("Insufficient funds"));
                    acc.setBalance(acc.getBalance().subtract(amount));
                    return repository.save(acc);
                });
    }
}
