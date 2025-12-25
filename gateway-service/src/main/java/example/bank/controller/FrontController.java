package example.bank.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;

import example.bank.model.TransferRequest;
import example.bank.service.GatewayAudit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping
public class FrontController {

    private final WebClient accountWebClient;
    private final WebClient exchangeWebClient;
    private final WebClient cashWebClient;
    private final WebClient transferWebClient;
    private final GatewayAudit audit;

    @Autowired
    public FrontController(
            @Qualifier("accountWebClient") WebClient accountWebClient,
            @Qualifier("exchangeWebClient") WebClient exchangeWebClient,
            @Qualifier("cashWebClient") WebClient cashWebClient,
            @Qualifier("transferWebClient") WebClient transferWebClient,
            GatewayAudit audit) {

        this.accountWebClient = accountWebClient;
        this.exchangeWebClient = exchangeWebClient;
        this.cashWebClient = cashWebClient;
        this.transferWebClient = transferWebClient;
        this.audit = audit;
    }

    @GetMapping("/bank")
    public Mono<Rendering> index(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            audit.warn("gateway.auth.required", "User is not authenticated, redirect to login");
            return Mono.just(Rendering.redirectTo("/oauth2/authorization/gateway-user").build());
        }

        String username = oidcUser.getPreferredUsername();
        audit.info("gateway.bank.view", "Load bank page for user=" + safe(username));

        Mono<Object> accountsMono = accountWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/accounts").queryParam("username", username).build())
                .retrieve()
                .bodyToMono(Object.class)
                .doOnError(e -> audit.error("gateway.accounts.fetch", "Failed to fetch accounts", e));

        Mono<Object> ratesMono = exchangeWebClient.get()
                .uri("/exchange/rates")
                .retrieve()
                .bodyToMono(Object.class)
                .doOnError(e -> audit.error("gateway.exchange.fetch", "Failed to fetch exchange rates", e));

        return Mono.zip(accountsMono, ratesMono)
                .map(tuple -> Rendering.view("bank")
                        .modelAttribute("accounts", tuple.getT1())
                        .modelAttribute("rates", tuple.getT2())
                        .build());
    }

    @GetMapping("/accounts/create")
    public Mono<RedirectView> createAccount(@AuthenticationPrincipal OidcUser oidcUser,
                                           @RequestParam String currency) {
        String username = oidcUser.getPreferredUsername();
        audit.info("gateway.account.create", "Create account requested for user=" + safe(username) + " currency=" + safe(currency));

        Map<String, Object> accountData = Map.of(
                "username", username,
                "currency", currency,
                "balance", BigDecimal.ZERO);

        return accountWebClient.post()
                .uri("/accounts/create")
                .bodyValue(accountData)
                .retrieve()
                .toBodilessEntity()
                .thenReturn(new RedirectView("/bank"))
                .doOnError(e -> audit.error("gateway.account.create", "Create account failed", e));
    }

    @GetMapping("/cash/deposit/{id}")
    public Mono<RedirectView> deposit(@AuthenticationPrincipal OidcUser oidcUser,
                                      @PathVariable Long id,
                                      @RequestParam BigDecimal amount) {

        if (oidcUser == null) {
            audit.warn("gateway.auth.required", "User is not authenticated, redirect to login");
            return Mono.just(new RedirectView("/oauth2/authorization/gateway-user"));
        }

        String username = oidcUser.getPreferredUsername();
        audit.info("gateway.cash.deposit", "Deposit requested accountId=" + id + " user=" + safe(username));

        return cashWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cash/deposit/{id}")
                        .queryParam("username", username)
                        .queryParam("amount", amount) // в запросе передаём, но НЕ логируем
                        .build(id))
                .retrieve()
                .toBodilessEntity()
                .thenReturn(new RedirectView("/bank"))
                .doOnError(e -> audit.error("gateway.cash.deposit", "Deposit failed", e));
    }

    @GetMapping("/cash/withdraw/{id}")
    public Mono<RedirectView> withdraw(@AuthenticationPrincipal OidcUser oidcUser,
                                       @PathVariable Long id,
                                       @RequestParam BigDecimal amount) {

        if (oidcUser == null) {
            audit.warn("gateway.auth.required", "User is not authenticated, redirect to login");
            return Mono.just(new RedirectView("/oauth2/authorization/gateway-user"));
        }

        String username = oidcUser.getPreferredUsername();
        audit.info("gateway.cash.withdraw", "Withdraw requested accountId=" + id + " user=" + safe(username));

        return cashWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/cash/withdraw/{id}")
                        .queryParam("username", username)
                        .queryParam("amount", amount)
                        .build(id))
                .retrieve()
                .toBodilessEntity()
                .thenReturn(new RedirectView("/bank"))
                .doOnError(e -> audit.error("gateway.cash.withdraw", "Withdraw failed", e));
    }

    @PostMapping("/transfer")
    public Mono<RedirectView> transfer(@AuthenticationPrincipal OidcUser oidcUser,
                                       @ModelAttribute TransferRequest request) {
        if (oidcUser == null) {
            audit.warn("gateway.auth.required", "User is not authenticated, redirect to login");
            return Mono.just(new RedirectView("/oauth2/authorization/gateway-user"));
        }

        String username = oidcUser.getPreferredUsername();
        request.setFromUsername(username);

        // amount НЕ логируем
        audit.info("gateway.transfer.create",
                "Transfer requested from=" + safe(username) + " to=" + safe(request.getToUsername()) + " toId=" + request.getToId());

        return transferWebClient.post()
                .uri("/transfer")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .thenReturn(new RedirectView("/bank"))
                .doOnError(e -> audit.error("gateway.transfer.create", "Transfer failed", e));
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }
}
