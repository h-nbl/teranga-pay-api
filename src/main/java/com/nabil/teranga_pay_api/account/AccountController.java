package com.nabil.teranga_pay_api.account;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getAccountOf(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/me/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public DepositResponse deposit(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody DepositRequest request) {
        return accountService.deposit(UUID.fromString(jwt.getSubject()), request.amount());
    }
}