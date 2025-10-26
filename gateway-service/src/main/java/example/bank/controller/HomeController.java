package example.bank.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import example.bank.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    

    @GetMapping({"/", "/user"})
    public String homePage(Model model, @AuthenticationPrincipal OidcUser oidcUser) {

        // Если пользователь уже аутентифицирован через Keycloak
        if (oidcUser != null) {
            log.info("Пользователь авторизован: {}", oidcUser.getPreferredUsername());
            // Можно сразу редиректить на /bank или показывать личные данные
            return "redirect:/bank";
        }

        // Если пользователь не авторизован — показать кнопку регистрации и кнопку логина
        model.addAttribute("user", new User());
        return "home"; // шаблон home.html
    }
}
