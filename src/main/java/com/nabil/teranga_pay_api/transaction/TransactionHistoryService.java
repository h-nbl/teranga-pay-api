package com.nabil.teranga_pay_api.transaction;

import com.nabil.teranga_pay_api.account.Account;
import com.nabil.teranga_pay_api.account.AccountNotFoundException;
import com.nabil.teranga_pay_api.account.AccountRepository;
import com.nabil.teranga_pay_api.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionHistoryService {

    private static final Instant BEGINNING = Instant.EPOCH;
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> history(UUID userId, TransactionType type,
                                                     Instant from, Instant to,
                                                     int page, int size) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new InvalidDateRangeException();
        }

        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(AccountNotFoundException::new);

        Collection<TransactionType> types = type == null
                ? EnumSet.allOf(TransactionType.class)
                : EnumSet.of(type);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<Transaction> result = transactionRepository.search(
                account.getId(), types,
                from == null ? BEGINNING : from,
                to == null ? FAR_FUTURE : to,
                (Pageable) pageable);

        return PageResponse.from(result.map(tx -> TransactionResponse.from(tx, account.getId())));
    }
}