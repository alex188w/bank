package example.bank.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import example.bank.model.OutboxEvent;
import reactor.core.publisher.Flux;

public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, Long> {

    Flux<OutboxEvent> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
