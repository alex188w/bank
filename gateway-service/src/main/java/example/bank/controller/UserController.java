package example.bank.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import example.bank.model.User;
import example.bank.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public Mono<RedirectView> registerUser(@Valid @ModelAttribute("user") User user,
            BindingResult result,
            Model model) {
        if (result.hasErrors()) {
            log.warn("Ошибка валидации при регистрации: {}", result.getAllErrors());
            model.addAttribute("user", user);
            return Mono.just(new RedirectView("/user/register?error"));
        }

        return userService.registerUser(user)
                .doOnSubscribe(s -> log.info("Регистрация пользователя: {}", user.getUsername()))
                .doOnSuccess(saved -> log.info("Пользователь успешно зарегистрирован: {}", saved.getUsername()))
                .doOnError(e -> log.error("Ошибка в сервисе регистрации: ", e))
                .thenReturn(new RedirectView("/bank"))
                .onErrorResume(ex -> {
                    log.error("Ошибка регистрации пользователя: ", ex);
                    return Mono.just(new RedirectView("/user/register?error"));
                });
    }

    @GetMapping("/profile")
    public Mono<String> profile(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        String username = oidcUser.getPreferredUsername();
        return userService.getProfile(username)
                .map(user -> {
                    model.addAttribute("user", user);
                    return "profile";
                });
    }

    @PostMapping("/profile")
    public Mono<RedirectView> updateProfile(@AuthenticationPrincipal OidcUser oidcUser,
            @ModelAttribute User updatedUser) {
        String username = oidcUser.getPreferredUsername();
        return userService.updateProfile(username, updatedUser)
                .thenReturn(new RedirectView("/user/profile?success"))
                .onErrorResume(e -> {
                    log.warn("Ошибка при обновлении профиля: {}", e.getMessage());
                    return Mono.just(new RedirectView("/user/profile?error"));
                });
    }

    // возвращаем access_token текущего пользователя
    @GetMapping("/api/token")
    public Map<String, String> getToken(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        if (client == null || client.getAccessToken() == null) {
            return Map.of("error", "Unauthorized");
        }
        return Map.of("access_token", client.getAccessToken().getTokenValue());
    }

    // имя пользователя (из Keycloak)
    @GetMapping("/api/user")
    public Map<String, String> getUser(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            return Map.of("name", "Guest");
        }
        String username = user.getAttribute("preferred_username");
        if (username == null)
            username = user.getName();
        return Map.of("name", username);
    }

}
