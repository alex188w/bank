package example.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final WebClient webClient;

    public Mono<Void> transfer(String fromUsername, Long fromId, String toUsername, Long toId, BigDecimal amount) {
        return getAccount(fromId)
                .flatMap(fromAccount -> {
                    BigDecimal balance = new BigDecimal(fromAccount.get("balance").toString());
                    if (balance.compareTo(amount) < 0) {
                        return Mono.error(new IllegalArgumentException("Недостаточно средств на счёте отправителя"));
                    }

                    return getAccount(toId)
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("Счёт получателя не найден")))
                            .then(withdraw(fromId, amount))
                            .then(deposit(toId, amount))
                            .doOnSuccess(v -> log.info("Перевод {} от {}({}) → {}({}) выполнен",
                                    amount, fromUsername, fromId, toUsername, toId));
                });
    }

    private Mono<Map<String, Object>> getAccount(Long id) {
        return webClient.get()
                .uri("http://localhost:8082/accounts/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .doOnNext(acc -> log.info("Найден счёт: {}", acc));
    }

    private Mono<Void> withdraw(Long id, BigDecimal amount) {
        return webClient.post()
                .uri("http://localhost:8082/accounts/{id}/withdraw?amount={amount}", id, amount)
                .retrieve()
                .bodyToMono(Void.class);
    }

    private Mono<Void> deposit(Long id, BigDecimal amount) {
        return webClient.post()
                .uri("http://localhost:8082/accounts/{id}/deposit?amount={amount}", id, amount)
                .retrieve()
                .bodyToMono(Void.class);
    }
}