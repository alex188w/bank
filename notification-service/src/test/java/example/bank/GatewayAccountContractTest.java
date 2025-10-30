package example.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.stubrunner.StubFinder;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringJUnitConfig
@AutoConfigureStubRunner(
        ids = "example.bank:account-service:+:stubs:8082",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
public class GatewayAccountContractTest {

    @Autowired
    private StubFinder stubFinder; // <- поле, не конструктор

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        // WebTestClient создаём вручную, без поднятия всего контекста
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + stubFinder.findStubUrl("example.bank", "account-service").getPort())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Test
    void shouldGetAccountById() {
        webTestClient.get()
                .uri("/accounts/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.username").isEqualTo("user1")
                .jsonPath("$.balance").isEqualTo(1000.50);
    }
}
