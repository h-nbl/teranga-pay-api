package com.nabil.teranga_pay_api.account;

import com.nabil.teranga_pay_api.transaction.Transaction;
import com.nabil.teranga_pay_api.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final WalletProperties walletProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AccountResponse getAccountOf(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(AccountNotFoundException::new);
        return AccountResponse.from(account);
    }

    @Transactional
    public DepositResponse deposit(UUID userId, long amount) {
        if (amount > walletProperties.maxDeposit()) {
            throw new DepositLimitExceededException(walletProperties.maxDeposit());
        }

        Account account = accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(AccountNotFoundException::new);

        account.credit(amount);
        Transaction deposit = transactionRepository.save(
                Transaction.deposit(account, amount, clock.instant()));

        log.info("Deposit completed: userId={} transactionId={} amount={}",
                userId, deposit.getId(), amount);

        return new DepositResponse(deposit.getId(), amount, account.getBalance(),
                AccountResponse.CURRENCY, deposit.getCreatedAt());
    }
}