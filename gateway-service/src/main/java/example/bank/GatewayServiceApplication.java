package example.bank;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			System.out.println("=== Registered Beans: ===");
			Arrays.stream(ctx.getBeanDefinitionNames())
					.filter(b -> b.contains("proxy") || b.contains("controller"))
					.sorted()
					.forEach(System.out::println);
		};
	}

}
