package example.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/exchange/**").permitAll() // временно, чтобы проверить
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt)
                .build();
    }
}


// @Configuration
// @EnableWebFluxSecurity
// public class SecurityConfig {

//     @Bean
//     public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//         http
//             .csrf(csrf -> csrf.disable())
//             .cors(cors -> cors.configurationSource(corsConfigurationSource())) // применяем кастомный CORS
//             .authorizeExchange(exchanges -> exchanges
//                 .pathMatchers(
//                     "/actuator/**",
//                     "/generate/**",
//                     "/exchange/rates", 
//                     "/exchange/update"  // разрешаем публично
//                 ).permitAll()
//                 .anyExchange().authenticated()
//             )
//             .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())));
//         return http.build();
//     }

//     @Bean
//     public ReactiveJwtDecoder jwtDecoder() {
//         return NimbusReactiveJwtDecoder.withJwkSetUri(
//                 "http://localhost:8090/realms/bank/protocol/openid-connect/certs"
//         ).build();
//     }

//     @Bean
//     public CorsConfigurationSource corsConfigurationSource() {
//         CorsConfiguration config = new CorsConfiguration();
//         config.setAllowedOrigins(List.of("http://localhost:8080")); // фронт
//         config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//         config.setAllowedHeaders(List.of("*"));
//         config.setAllowCredentials(true);

//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//         source.registerCorsConfiguration("/**", config);
//         return source;
//     }
// }
