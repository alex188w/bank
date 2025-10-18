package example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class SecurityConfig {

    private static final String ISSUER_URI = "http://localhost:8090/realms/bank"; 

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // отключаем CSRF для микросервисов
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated() // все запросы требуют аутентификации
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Spring Security сам подтягивает публичный ключ через /.well-known/openid-configuration
        return NimbusJwtDecoder.withJwkSetUri(ISSUER_URI + "/protocol/openid-connect/certs").build();
    }
}
