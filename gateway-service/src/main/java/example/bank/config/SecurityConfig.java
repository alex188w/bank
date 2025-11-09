package example.bank.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String ISSUER_URI = "http://192.168.0.140:8090/realms/bank";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/", "/css/**", "/js/**", "/images/**", "/user/**", "register/**",
                                "/bank/api/user", "/api/user")
                        .permitAll()
                        .pathMatchers("/bank/**", "/cash/**", "/accounts/**").authenticated()
                        .anyExchange().authenticated())
                // OAuth2 Login — Keycloak страница входа
                .oauth2Login(Customizer.withDefaults())

                // OAuth2 Client — для вызовов к другим микросервисам от имени пользователя
                .oauth2Client(Customizer.withDefaults())

                // Logout — выходим из Keycloak и возвращаемся на главную
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((exchange, authentication) -> {
                            URI logoutUri = URI.create(
                                    "http://192.168.0.140:8090/realms/bank/protocol/openid-connect/logout?redirect_uri=http://localhost:8080/");
                            exchange.getExchange().getResponse().setStatusCode(HttpStatus.SEE_OTHER);
                            exchange.getExchange().getResponse().getHeaders().setLocation(logoutUri);
                            return exchange.getExchange().getResponse().setComplete();
                        }))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return ReactiveJwtDecoders.fromIssuerLocation(ISSUER_URI);
    }
}
