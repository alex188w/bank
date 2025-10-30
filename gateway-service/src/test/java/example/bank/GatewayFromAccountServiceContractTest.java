package example.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringJUnitConfig
@AutoConfigureStubRunner(ids = {
        "example.bank:account-service:+:stubs:8082",
        "example.bank:exchange-service:+:stubs:8084",
        "example.bank:cash-service:+:stubs:8083",
        "example.bank:transfer-service:+:stubs:8086"
}, stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class GatewayFromAccountServiceContractTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        // WebTestClient создаётся вручную, без контекста
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:8082") // обращаемся напрямую к стабу
                .build();
    }

    @Test
    void shouldGetBalanceFromAccountService() {
        webTestClient.get()
                .uri("/accounts/1") // путь строго как в контракте
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(1000.50)
                .jsonPath("$.currency").isEqualTo("USD");
    }
}
