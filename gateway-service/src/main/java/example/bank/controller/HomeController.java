package example.bank.controller;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import example.bank.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String homePage(Model model, @AuthenticationPrincipal OidcUser user) {
        if (user != null) {
            model.addAttribute("username", user.getPreferredUsername());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("fullName", user.getFullName());
        }
        model.addAttribute("appName", "Банковская платформа");
        return "home";
    }
}
