package example.bank.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import example.bank.model.User;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByUsername(String username);
    Mono<Void> deleteByUsername(String username);
}
