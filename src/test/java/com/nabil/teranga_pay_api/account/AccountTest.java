package com.nabil.teranga_pay_api.account;

import com.nabil.teranga_pay_api.user.Role;
import com.nabil.teranga_pay_api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        User user = new User("Awa Diop", "+221771234567", "hash", Role.USER, Instant.now());
        account = new Account(user, Instant.now());
    }

    @Test
    void creditThenDebitGivesCorrectBalance() {
        account.credit(10_000);
        account.debit(3_000);
        assertEquals(7_000, account.getBalance());
    }

    @Test
    void debitMoreThanBalanceIsRefusedAndBalanceIsUnchanged() {
        account.credit(1_000);

        assertThrows(IllegalStateException.class, () -> account.debit(5_000));

        assertEquals(1_000, account.getBalance());
    }

    @Test
    void creditWithZeroOrNegativeAmountIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> account.credit(0));
        assertThrows(IllegalArgumentException.class, () -> account.credit(-5));
        assertEquals(0, account.getBalance());
    }

    @Test
    void debitWithZeroOrNegativeAmountIsRefused() {
        account.credit(1_000);

        assertThrows(IllegalArgumentException.class, () -> account.debit(0));
        assertThrows(IllegalArgumentException.class, () -> account.debit(-5));
        assertEquals(1_000, account.getBalance());
    }

    @Test
    void creditBeyondLongMaxValueIsRefusedAndBalanceIsUnchanged() {
        account.credit(Long.MAX_VALUE);

        assertThrows(ArithmeticException.class, () -> account.credit(1));

        assertEquals(Long.MAX_VALUE, account.getBalance());
    }
}