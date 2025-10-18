package example.bank.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Slf4j
@Controller
@RequiredArgsConstructor
// @RequestMapping
public class FrontController {

        private final WebClient accountWebClient;
        private final WebClient cashWebClient;
        private final WebClient exchangeWebClient;

        // Главная страница — реактивно подгружаем счета и курсы
        @GetMapping("/bank")
        public Mono<Rendering> index() {
                log.info("Request to get accounts");
                Mono<Object> accountsMono = accountWebClient.get()
                                .uri("/accounts")
                                .retrieve()
                                .bodyToMono(Object.class);
                Mono<Object> ratesMono = exchangeWebClient.get()
                                .uri("/exchange/rates")
                                .retrieve()
                                .bodyToMono(Object.class);
                return Mono.zip(accountsMono, ratesMono)
                                .map(tuple -> Rendering.view("bank")
                                                .modelAttribute("accounts", tuple.getT1())
                                                .modelAttribute("rates", tuple.getT2())
                                                .build());
        }

        // Создание счета
        @PostMapping("/accounts")
        public Mono<RedirectView> createAccount(@RequestParam String username, @RequestParam String currency) {
                log.info("Request to get accounts");
                return accountWebClient.post()
                                .uri(uriBuilder -> uriBuilder.path("/accounts")
                                                .queryParam("username", username)
                                                .queryParam("currency", currency)
                                                .build())
                                .retrieve()
                                .toBodilessEntity()
                                .thenReturn(new RedirectView("/"));
        }

        // Пополнение счета
        @GetMapping("/cash/deposit/{id}")
        public Mono<RedirectView> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
                log.info("Пополнение через форму: id={}, amount={}", id, amount);
                return cashWebClient.post()
                                .uri(uriBuilder -> uriBuilder.path("/cash/deposit/{id}")
                                        .queryParam("amount", amount)
                                        .build(id))
                                .retrieve().toBodilessEntity()
                                .thenReturn(new RedirectView("/bank"));
        }

        // Снятие со счета
        @GetMapping("/cash/withdraw/{id}")
        public Mono<RedirectView> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
                log.info("Пополнение через форму: id={}, amount={}", id, amount);
                return cashWebClient.post()
                                .uri(uriBuilder -> uriBuilder.path("/cash/withdraw/{id}")
                                        .queryParam("amount", amount)
                                        .build(id))
                                .retrieve().toBodilessEntity()
                                .thenReturn(new RedirectView("/bank"));
        }

        // Logout — редирект на Keycloak
        @GetMapping("/logout")
        public RedirectView logout() {
                String redirectUri = "http://localhost:8080/";
                String logoutUrl = "http://localhost:8090/realms/bank/protocol/openid-connect/logout";
                return new RedirectView(logoutUrl + "?redirect_uri=" + redirectUri);
        }
}
