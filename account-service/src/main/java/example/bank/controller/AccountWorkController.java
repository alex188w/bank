package example.bank.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountWorkController {

    @GetMapping("/test")
    public String test() {
        return "account-work";
    }
}
