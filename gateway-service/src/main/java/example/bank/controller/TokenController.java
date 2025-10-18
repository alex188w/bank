package example.bank.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

// @RestController
// public class TokenController {

//     // возвращаем access_token текущего пользователя
//     @GetMapping("/api/token")
//     public Map<String, String> getToken(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
//         if (client == null || client.getAccessToken() == null) {
//             return Map.of("error", "Unauthorized");
//         }
//         return Map.of("access_token", client.getAccessToken().getTokenValue());
//     }

//     // имя пользователя (из Keycloak)
//     @GetMapping("/api/user")
//     public Map<String, String> getUser(@AuthenticationPrincipal OAuth2User user) {
//         if (user == null) {
//             return Map.of("name", "Guest");
//         }
//         String username = user.getAttribute("preferred_username");
//         if (username == null)
//             username = user.getName();
//         return Map.of("name", username);
//     }
// }
