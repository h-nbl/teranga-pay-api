package com.nabil.teranga_pay_api.user;

import com.nabil.teranga_pay_api.TestcontainersConfiguration;
import com.nabil.teranga_pay_api.account.Account;
import com.nabil.teranga_pay_api.account.AccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savedUserAndAccountCanBeReloadedFromDatabase() {
        User user = userRepository.save(
                new User("Awa Diop", "+221771234567", "hash", Role.USER, Instant.now()));
        accountRepository.save(new Account(user, Instant.now()));

        entityManager.flush();
        entityManager.clear();

        User reloadedUser = userRepository.findByPhone("+221771234567").orElseThrow();
        Account reloadedAccount = accountRepository.findByUserId(reloadedUser.getId()).orElseThrow();

        assertEquals("Awa Diop", reloadedUser.getFullName());
        assertEquals(Role.USER, reloadedUser.getRole());
        assertEquals(0, reloadedAccount.getBalance());
        assertEquals(0L, reloadedAccount.getVersion());
    }

    @Test
    void phoneNotInInternationalFormatIsRejectedByDatabase() {
        User badUser = new User("Awa Diop", "77 123 45 67", "hash", Role.USER, Instant.now());

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(badUser));
    }
}