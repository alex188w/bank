package example.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringJUnitConfig
@AutoConfigureStubRunner(ids = {
        "example.bank:account-service:+:stubs:8082",
        "example.bank:exchange-service:+:stubs:8084",
        "example.bank:cash-service:+:stubs:8083",
        "example.bank:transfer-service:+:stubs:8086"
}, stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class GatewayFromExchangeServiceContractTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        // WebTestClient создаётся вручную, без контекста
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:8084") // обращаемся напрямую к стабу
                .build();
    }

    @Test
    void shouldGetExchangeRate() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/exchange/rate")
                        .queryParam("from", "USD")
                        .queryParam("to", "EUR")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBody()
                .jsonPath("$.rate").isEqualTo(0.92);
    }
}
