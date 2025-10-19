package example.bank.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.RedirectView;

import example.bank.dto.TransferRequest;
import example.bank.service.TransferService;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public Mono<Void> transfer(@RequestBody TransferRequest request) {
        log.info("Transfer request: {} → {}({}), сумма={}",
                request.getToUsername(), request.getToUsername(),
                request.getToId(), request.getAmount());

        return transferService.transfer(
                request.getToUsername(),
                request.getFromId(),
                request.getToUsername(),
                request.getToId(),
                request.getAmount()
        );
    }
}
