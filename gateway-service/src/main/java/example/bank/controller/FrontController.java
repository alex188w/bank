package example.bank.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;

import example.bank.model.TransferRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping
public class FrontController {

        private final WebClient accountWebClient;
        private final WebClient exchangeWebClient;
        private final WebClient cashWebClient;
        private final WebClient transferWebClient;

        // Главная страница — реактивно подгружаем свои счета и курсы
        @GetMapping("/bank")
        public Mono<Rendering> index(@AuthenticationPrincipal OidcUser oidcUser) {

                if (oidcUser == null) {
                        log.warn("Пользователь не аутентифицирован, редиректим на логин");
                        return Mono.just(Rendering.redirectTo("/oauth2/authorization/gateway-user").build());
                }

                String username = oidcUser.getPreferredUsername();
                log.info("Загружаем данные для пользователя: {}", username);

                // Запрашиваем только свои счета у account-service
                Mono<Object> accountsMono = accountWebClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/accounts")
                                                .queryParam("username", username) // фильтруем по username
                                                .build())
                                .retrieve()
                                .bodyToMono(Object.class)
                                .doOnError(e -> log.error("Ошибка при получении счетов", e));

                // Запрашиваем курсы валют
                Mono<Object> ratesMono = exchangeWebClient.get()
                                .uri("/exchange/rates")
                                .retrieve()
                                .bodyToMono(Object.class)
                                .doOnError(e -> log.error("Ошибка при получении курсов", e));

                // Собираем данные в модель для шаблона
                return Mono.zip(accountsMono, ratesMono)
                                .map(tuple -> Rendering.view("bank")
                                                .modelAttribute("accounts", tuple.getT1())
                                                .modelAttribute("rates", tuple.getT2())
                                                .build());
        }

        // Создание нового счета
        @GetMapping("/accounts/create")
        public Mono<RedirectView> createAccount(@AuthenticationPrincipal OidcUser oidcUser,
                        @RequestParam String currency) {

                String username = oidcUser.getPreferredUsername();
                log.info("Создание нового счета для пользователя: {}", username);

                Map<String, Object> accountData = Map.of(
                                "username", username,
                                "currency", currency,
                                "balance", BigDecimal.ZERO);

                return accountWebClient.post()
                                .uri("/accounts/create") // <- здесь нужно /create
                                .bodyValue(accountData)
                                .retrieve()
                                .toBodilessEntity()
                                .thenReturn(new RedirectView("/bank"));
        }

        // Пополнение счета
        @GetMapping("/cash/deposit/{id}")
        public Mono<RedirectView> deposit(@AuthenticationPrincipal OidcUser oidcUser,
                        @PathVariable Long id,
                        @RequestParam BigDecimal amount) {

                if (oidcUser == null) {
                        log.warn("Пользователь не аутентифицирован, редиректим на логин");
                        return Mono.just(new RedirectView("/oauth2/authorization/gateway-user"));
                }

                String username = oidcUser.getPreferredUsername();
                log.info("Пополнение счета id={} для пользователя {}, сумма={}", id, username, amount);

                return cashWebClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/cash/deposit/{id}")
                                                .queryParam("username", username) // передаём username
                                                .queryParam("amount", amount)
                                                .build(id))
                                .retrieve()
                                .toBodilessEntity()
                                .thenReturn(new RedirectView("/bank"));
        }

        // Снятие со счета
        @GetMapping("/cash/withdraw/{id}")
        public Mono<RedirectView> withdraw(@AuthenticationPrincipal OidcUser oidcUser,
                        @PathVariable Long id,
                        @RequestParam BigDecimal amount) {

                if (oidcUser == null) {
                        log.warn("Пользователь не аутентифицирован, редиректим на логин");
                        return Mono.just(new RedirectView("/oauth2/authorization/gateway-user"));
                }

                String username = oidcUser.getPreferredUsername();
                log.info("Снятие со счета id={} для пользователя {}, сумма={}", id, username, amount);

                return cashWebClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/cash/withdraw/{id}")
                                                .queryParam("username", username) // передаём username
                                                .queryParam("amount", amount)
                                                .build(id))
                                .retrieve()
                                .toBodilessEntity()
                                .thenReturn(new RedirectView("/bank"));
        }

        // имя пользователя (из Keycloak)
        @GetMapping("/api/user")
        @ResponseBody
        public Mono<Map<String, String>> getUser(@AuthenticationPrincipal Mono<OAuth2User> userMono) {
                return userMono
                                .map(user -> {
                                        String username = user.getAttribute("preferred_username");
                                        if (username == null)
                                                username = user.getName();
                                        return Map.of("name", username);
                                })
                                .defaultIfEmpty(Map.of("name", "Guest"));
        }

        @PostMapping("/transfer")
        public Mono<RedirectView> transfer(@AuthenticationPrincipal OidcUser oidcUser,
                        @ModelAttribute TransferRequest request) {
                if (oidcUser == null) {
                        log.warn("Пользователь не аутентифицирован, редиректим на логин");
                        return Mono.just(new RedirectView("/oauth2/authorization/gateway-user"));
                }

                String username = oidcUser.getPreferredUsername();
                request.setFromUsername(username); // задаём отправителя

                log.info("Перевод запроса: {} → {}({}), сумма={}",
                                username, request.getToUsername(), request.getToId(), request.getAmount());

                return transferWebClient.post()
                                .uri("/transfer")
                                .bodyValue(request)
                                .retrieve()
                                .toBodilessEntity()
                                .thenReturn(new RedirectView("/bank"));
        }

}
