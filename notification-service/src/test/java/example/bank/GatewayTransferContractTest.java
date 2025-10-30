package example.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringJUnitConfig
@AutoConfigureStubRunner(
    ids = "example.bank:transfer-service:+:stubs:8086",
    stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
public class GatewayTransferContractTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        // WebTestClient создаётся вручную, без поднятия контекста
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:8086") // stub transfer-service
                .build();
    }

    @Test
    void shouldTransferMoney() {
        webTestClient.post()
                .uri("/transfer") // URL из контракта
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "fromId": 1,
                          "fromUsername": "user1",
                          "toId": 2,
                          "toUsername": "user2",
                          "amount": 100.50
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }
}
