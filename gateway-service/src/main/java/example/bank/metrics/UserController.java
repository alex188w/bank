package example.bank.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final MeterRegistry meterRegistry;

    @GetMapping("/api/user")
    public Map<String, String> getUser(@AuthenticationPrincipal OAuth2User user) {

        if (user == null) {
            Counter.builder("auth_userinfo_total")
                    .tag("result", "guest")
                    .tag("login", "unknown")
                    .register(meterRegistry)
                    .increment();
            return Map.of("name", "Guest");
        }

        String username = user.getAttribute("preferred_username");
        if (username == null || username.isBlank()) {
            username = user.getName();
        }

        Counter.builder("auth_userinfo_total")
                .tag("result", "success")
                .tag("login", safe(username))
                .register(meterRegistry)
                .increment();

        return Map.of("name", username);
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }
}
