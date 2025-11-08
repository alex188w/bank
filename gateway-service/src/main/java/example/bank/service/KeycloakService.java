package example.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

// import example.bank.config.KeycloakWebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {
        private final @Qualifier("keycloakWebClient") WebClient keycloakWebClient;
        @Value("${keycloak.server-url}")
        private String keycloakServerUrl;
        @Value("${keycloak.realm}")
        private String realm;
        @Value("${keycloak.client-id}")
        private String clientId;
        @Value("${keycloak.client-secret}")
        private String clientSecret;

        /** * Проверяет, существует ли пользователь в Keycloak */
        public Mono<Boolean> userExistsInKeycloak(String username) {
                return getAdminToken()
                                .flatMap(token -> keycloakWebClient.get()
                                                .uri(keycloakServerUrl + "/admin/realms/" + realm + "/users?username="
                                                                + username)
                                                .header("Authorization", "Bearer " + token)
                                                .retrieve()
                                                .bodyToFlux(Object.class)
                                                .hasElements() // true, если хотя бы один пользователь найден
                                );
        }

        /**
         * Создаёт пользователя в Keycloak и устанавливает пароль
         */
        public Mono<Void> createUserInKeycloak(String username, String email, String password) {
                return getAdminToken()
                                .flatMap(token -> {
                                        Map<String, Object> userPayload = Map.of(
                                                        "username", username,
                                                        "email", email,
                                                        "enabled", true,
                                                        "credentials", new Object[] {
                                                                        Map.of(
                                                                                        "type", "password",
                                                                                        "value", password,
                                                                                        "temporary", false)
                                                        });
                                        return keycloakWebClient.post()
                                                        .uri(keycloakServerUrl + "/admin/realms/" + realm + "/users")
                                                        .header("Authorization", "Bearer " + token)
                                                        .bodyValue(userPayload)
                                                        .retrieve()
                                                        .toBodilessEntity()
                                                        .then();
                                })
                                .onErrorResume(WebClientResponseException.class, ex -> {
                                        log.error("Ошибка Keycloak: {} {}", ex.getStatusCode(),
                                                        ex.getResponseBodyAsString());
                                        return Mono.error(ex);
                                });
        }

        /**
         * Получаем токен администратора через client_credentials
         */
        private Mono<String> getAdminToken() {
                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("grant_type", "client_credentials");
                formData.add("client_id", clientId);
                formData.add("client_secret", clientSecret);

                return keycloakWebClient.post()
                                .uri(keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue(formData)
                                .retrieve()
                                .bodyToMono(Map.class)
                                .map(body -> (String) body.get("access_token"))
                                .doOnError(e -> log.error("Не удалось получить токен Keycloak", e));
        }
}
