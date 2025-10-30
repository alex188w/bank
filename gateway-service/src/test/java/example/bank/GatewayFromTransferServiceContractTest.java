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
@AutoConfigureStubRunner(ids = "example.bank:transfer-service:+:stubs", stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class GatewayFromTransferServiceContractTest {

    private WebTestClient webTestClient;

    @Autowired
    private StubFinder stubFinder;

    @BeforeEach
    void setup() {
        // Получаем URL с портом, на котором поднят stub
        String baseUrl = stubFinder.findStubUrl("transfer-service").toString();
        webTestClient = WebTestClient.bindToServer()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Test
    void shouldTransferMoney() {
        webTestClient.post()
                .uri("/transfer")
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
