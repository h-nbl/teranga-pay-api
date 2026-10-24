package com.nabil.teranga_pay_api.auth;

import com.nabil.teranga_pay_api.TestcontainersConfiguration;
import com.nabil.teranga_pay_api.account.Account;
import com.nabil.teranga_pay_api.account.AccountRepository;
import com.nabil.teranga_pay_api.common.InvalidPhoneNumberException;
import com.nabil.teranga_pay_api.user.Role;
import com.nabil.teranga_pay_api.user.User;
import com.nabil.teranga_pay_api.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void registerCreatesUserAndEmptyAccountWithNormalizedPhone() {
        RegisterResponse response = authService.register(
                new RegisterRequest("  Awa Diop ", "77 123 45 67", "motdepasse123"));

        entityManager.flush();
        entityManager.clear();

        User user = userRepository.findByPhone("+221771234567").orElseThrow();
        Account account = accountRepository.findByUserId(user.getId()).orElseThrow();

        assertEquals(response.userId(), user.getId());
        assertEquals("Awa Diop", user.getFullName());
        assertEquals(Role.USER, user.getRole());
        assertEquals(0, account.getBalance());
    }

    @Test
    void passwordIsStoredHashedWithBcrypt() {
        authService.register(new RegisterRequest("Awa Diop", "+221771234567", "motdepasse123"));

        entityManager.flush();
        entityManager.clear();

        User user = userRepository.findByPhone("+221771234567").orElseThrow();

        assertNotEquals("motdepasse123", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches("motdepasse123", user.getPasswordHash()));
        assertFalse(passwordEncoder.matches("autre-mot-de-passe", user.getPasswordHash()));
    }

    @Test
    void sameNumberWrittenDifferentlyIsRefusedAsDuplicate() {
        authService.register(new RegisterRequest("Awa Diop", "+221771234567", "motdepasse123"));

        assertThrows(PhoneAlreadyRegisteredException.class, () ->
                authService.register(
                        new RegisterRequest("Autre Personne", "77 123 45 67", "motdepasse456")));
    }

    @Test
    void invalidPhoneIsRefusedAndNothingIsSaved() {
        long usersBefore = userRepository.count();

        assertThrows(InvalidPhoneNumberException.class, () ->
                authService.register(new RegisterRequest("Awa Diop", "12345", "motdepasse123")));

        assertEquals(usersBefore, userRepository.count());
    }
}