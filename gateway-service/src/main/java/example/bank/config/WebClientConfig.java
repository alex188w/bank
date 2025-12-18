package example.bank.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import brave.Tracer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import brave.Tracer;

import io.micrometer.observation.ObservationRegistry;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

        private final ReactiveClientRegistrationRepository clients;
        private final ReactiveOAuth2AuthorizedClientService authService;

        // ВАЖНО: инжектим builder от Spring (он уже с tracing/metrics)
        private final WebClient.Builder webClientBuilder;

        private final ObservationRegistry observationRegistry;

        private final Tracer tracer;

        @Value("${clients.account.base-url:http://bank-platform-account-service:8082}")
        private String accountBaseUrl;

        @Value("${clients.exchange.base-url:http://bank-platform-exchange-service:8084}")
        private String exchangeBaseUrl;

        @Value("${clients.cash.base-url:http://bank-platform-cash-service:8083}")
        private String cashBaseUrl;

        @Value("${clients.transfer.base-url:http://bank-platform-transfer-service:8086}")
        private String transferBaseUrl;

        @Value("${clients.notification.base-url:http://bank-platform-notification-service:8087}")
        private String notificationBaseUrl;

        private ExchangeFilterFunction b3PropagationFilter(Tracer tracer) {
                return (request, next) -> {
                        var span = tracer.currentSpan();
                        if (span == null)
                                return next.exchange(request);

                        var ctx = span.context();
                        ClientRequest newReq = ClientRequest.from(request)
                                        .headers(h -> {
                                                h.set("X-B3-TraceId", ctx.traceIdString());
                                                h.set("X-B3-SpanId", ctx.spanIdString());
                                                if (ctx.parentIdString() != null) {
                                                        h.set("X-B3-ParentSpanId", ctx.parentIdString());
                                                }
                                                h.set("X-B3-Sampled", "1");
                                        })
                                        .build();

                        return next.exchange(newReq);
                };
        }

        private WebClient buildWebClient(String baseUrl, String clientRegistrationId) {
                var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clients, authService);
                var oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
                oauth2.setDefaultClientRegistrationId(clientRegistrationId);
                return webClientBuilder
                                .clone()
                                .observationRegistry(observationRegistry)
                                .baseUrl(baseUrl)
                                .filter(b3PropagationFilter(tracer))
                                .filter(oauth2)
                                .build();
        }

        @Bean("accountWebClient")
        public WebClient accountWebClient() {
                return buildWebClient(accountBaseUrl, "gateway-client");
        }

        @Bean("cashWebClient")
        public WebClient cashWebClient() {
                return buildWebClient(cashBaseUrl, "gateway-client");
        }

        @Bean("exchangeWebClient")
        public WebClient exchangeWebClient() {
                return buildWebClient(exchangeBaseUrl, "gateway-client");
        }

        @Bean("transferWebClient")
        public WebClient transferWebClient() {
                return buildWebClient(transferBaseUrl, "gateway-client");
        }

        @Bean("notificationWebClient")
        public WebClient notificationWebClient() {
                return webClientBuilder
                                .clone()
                                .observationRegistry(observationRegistry)
                                .baseUrl(notificationBaseUrl)
                                .build();
        }

        @Bean
        public WebClient keycloakWebClient() {
                return webClientBuilder.clone().build();
        }
}
