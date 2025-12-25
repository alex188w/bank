package example.bank.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
public class ReactorMdcConfig {

    @PostConstruct
    public void setup() {
        // включает перенос контекста между потоками в Reactor
        Hooks.enableAutomaticContextPropagation();
    }
}
