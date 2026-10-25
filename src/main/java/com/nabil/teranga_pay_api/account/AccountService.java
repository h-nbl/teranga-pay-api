package com.nabil.teranga_pay_api.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public AccountResponse getAccountOf(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(AccountNotFoundException::new);
        return AccountResponse.from(account);
    }
}