package example.bank.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import example.bank.model.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {
    Flux<Account> findByUsername(String username);     
    Mono<Account> findByIdAndUsername(Long id, String username);
}
