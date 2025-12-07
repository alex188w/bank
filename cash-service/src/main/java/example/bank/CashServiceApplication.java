package example.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
// @EnableDiscoveryClient  // <- обязательно для регистрации в Consul и работы lb://
public class CashServiceApplication {
    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(CashServiceApplication.class, args);
    }
}
