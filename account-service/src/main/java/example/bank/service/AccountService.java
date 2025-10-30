package example.bank.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import example.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository; // если используешь R2DBC/JDBC

    public Mono<Void> deposit(Long accountId, BigDecimal amount) {
        return accountRepository.findById(accountId)
                .switchIfEmpty(Mono.error(new RuntimeException("Account not found")))
                .flatMap(acc -> {
                    acc.setBalance(acc.getBalance().add(amount));
                    return accountRepository.save(acc);
                })
                .then();
    }

    public Mono<Void> withdraw(Long accountId, BigDecimal amount) {
        return accountRepository.findById(accountId)
                .switchIfEmpty(Mono.error(new RuntimeException("Account not found")))
                .flatMap(acc -> {
                    if (acc.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new RuntimeException("Insufficient funds"));
                    }
                    acc.setBalance(acc.getBalance().subtract(amount));
                    return accountRepository.save(acc);
                })
                .then();
    }
}