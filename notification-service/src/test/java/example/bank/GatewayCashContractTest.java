package example.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringJUnitConfig
@AutoConfigureStubRunner(
    ids = "example.bank:cash-service:+:stubs:8083",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
public class GatewayCashContractTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        // WebTestClient создаётся вручную, без поднятия контекста
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:8083") // stub cash-service
                .build();
    }

    @Test
    void shouldGetCashBalance() {
        webTestClient.get()
                .uri("/cash/balance/1") // URL из контракта
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.accountId").isEqualTo(1)
                .jsonPath("$.balance").isEqualTo(1000.50);
    }
}
