package example.bank.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProxyController {

        private final WebClient accountWebClient;
        private final WebClient cashWebClient;
        private final WebClient exchangeWebClient;

        // Проксируем accounts
        @GetMapping("/accounts")
        public Mono<Object> getAccounts() {
                return accountWebClient.get()
                                .uri("/accounts") // относительно baseUrl
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                                                .flatMap(body -> Mono.error(new RuntimeException(
                                                                "Exchange service error: " + body))))
                                .bodyToMono(Object.class);
        }

        // Пополнение счета
        @PostMapping("/cash/deposit/{id}")
        public Mono<Object> deposit(@PathVariable Long id, @RequestParam BigDecimal amount) {
                return cashWebClient.post()
                                .uri(uriBuilder -> uriBuilder.path("/cash/deposit/{id}").queryParam("amount", amount)
                                                .build(id))
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                                                .flatMap(body -> Mono.error(new RuntimeException(
                                                                "Exchange service error: " + body))))
                                .bodyToMono(Object.class);
        }

        // Снятие со счета
        @PostMapping("/accounts/{id}/withdraw")
        public Mono<Object> withdraw(@PathVariable Long id, @RequestParam BigDecimal amount) {
                return accountWebClient.post()
                                .uri(uriBuilder -> uriBuilder.path("/accounts/{id}/withdraw")
                                                .queryParam("amount", amount).build(id))
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                                                .flatMap(body -> Mono.error(new RuntimeException(
                                                                "Exchange service error: " + body))))
                                .bodyToMono(Object.class);
        }

        // Проксируем курсы валют
        @GetMapping("/exchange/rates")
        public Mono<Object> getRates() {
                return exchangeWebClient.get()
                                .uri("/exchange/rates")
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                                                .flatMap(body -> Mono.error(new RuntimeException(
                                                                "Exchange service error: " + body))))
                                .bodyToMono(Object.class);
        }

        @GetMapping("/test")
        public Mono<Map<String, String>> test() {
                return Mono.just(Map.of("status", "Proxy works!"));
        }

        // возвращаем access_token текущего пользователя
        @GetMapping("/token")
        public Map<String, String> getToken(
                        @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
                if (client == null || client.getAccessToken() == null) {
                        return Map.of("error", "Unauthorized");
                }
                return Map.of("access_token", client.getAccessToken().getTokenValue());
        }

        // имя пользователя (из Keycloak)
        @GetMapping("/user")
        public Map<String, String> getUser(@AuthenticationPrincipal OAuth2User user) {
                if (user == null) {
                        return Map.of("name", "Guest");
                }
                String username = user.getAttribute("preferred_username");
                if (username == null)
                        username = user.getName();
                return Map.of("name", username);
        }

        @GetMapping("/debug-token")
        public Mono<String> debugToken(
                        @RegisteredOAuth2AuthorizedClient("gateway-client") OAuth2AuthorizedClient client) {
                return Mono.just("Token: " + client.getAccessToken().getTokenValue());
        }

        @GetMapping("/debug-accounts")
        public Mono<Object> debugAccounts() {
                return accountWebClient.get()
                                .uri("/accounts")
                                .retrieve()
                                .bodyToMono(Object.class);
        }
}
