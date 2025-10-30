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
        "example.bank:cash-service:+:stubs:8083"
}, stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class GatewayFromCashServiceContractTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        // WebTestClient создаётся вручную, без контекста
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:8083") // обращаемся напрямую к стабу
                .build();
    }

    @Test
    void shouldGetCashBalance() {
        webTestClient.get()
                .uri("/cash/balance/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectBody()
                .jsonPath("$.balance").isEqualTo(1000.50);
    }
}
