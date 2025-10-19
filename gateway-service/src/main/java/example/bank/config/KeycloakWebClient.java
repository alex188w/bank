package example.bank.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// @Component
// @RequiredArgsConstructor
// public class KeycloakWebClient {

//     @Value("${keycloak.base-url}")
//     private String keycloakBaseUrl;

//     @Value("${keycloak.realm}")
//     private String realm;

//     private final WebClient webClient = WebClient.create();

//     public Mono<Void> createUser(String username, String email, String password) {
//         return webClient.post()
//                 .uri(keycloakBaseUrl + "/admin/realms/" + realm + "/users")
//                 .bodyValue("""
//                     {
//                         "username": "%s",
//                         "email": "%s",
//                         "enabled": true,
//                         "credentials": [{"type": "password", "value": "%s", "temporary": false}]
//                     }
//                 """.formatted(username, email, password))
//                 .retrieve()
//                 .bodyToMono(Void.class);
//     }

//     public Mono<Object> findUserByUsername(String username) {
//         return webClient.get()
//                 .uri(keycloakBaseUrl + "/admin/realms/" + realm + "/users?username=" + username)
//                 .retrieve()
//                 .bodyToFlux(Object.class)
//                 .next(); // если список не пуст — пользователь найден
//     }
// }
