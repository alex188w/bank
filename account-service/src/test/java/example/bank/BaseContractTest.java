package example.bank;

import reactor.core.publisher.Mono;
import example.bank.controller.AccountController;
import example.bank.model.Account;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;

import example.bank.repository.AccountRepository;
import example.bank.service.AccountService;


@WebFluxTest(AccountController.class)
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        ReactiveSecurityAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
public abstract class BaseContractTest {

    public static Object webTestClient;

    @MockitoBean
    protected AccountRepository repository;

    @MockitoBean
    protected AccountService service;

    @MockitoBean
    protected WebClient notificationWebClient;

    @BeforeEach
    public void setup() {
        // Создаём моковый объект Account
        Account account = new Account();
        account.setId(1L);
        account.setUsername("user1");
        account.setOwnerId("user1");
        account.setBalance(BigDecimal.valueOf(1000.50));
        account.setCurrency("USD");

        // Мокаем репозиторий
        Mockito.when(repository.findById(1L))
               .thenReturn(Mono.just(account));
    }
}
