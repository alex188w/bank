package example.bank;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class BaseContractTest {

    @Autowired
    protected WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        // Настраиваем только WebTestClient для stub-портов
        webTestClient = webTestClient.mutate()
            .baseUrl("http://localhost:8081") // account-service stub порт
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
