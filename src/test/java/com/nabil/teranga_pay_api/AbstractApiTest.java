package com.nabil.teranga_pay_api;

import com.nabil.teranga_pay_api.account.Account;
import com.nabil.teranga_pay_api.account.AccountRepository;
import com.nabil.teranga_pay_api.auth.TokenService;
import com.nabil.teranga_pay_api.user.Role;
import com.nabil.teranga_pay_api.user.User;
import com.nabil.teranga_pay_api.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class AbstractApiTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected TokenService tokenService;

    protected TestUser createUser(String fullName, String phone) {
        return createUser(fullName, phone, Role.USER);
    }

    protected TestUser createUser(String fullName, String phone, Role role) {
        Instant now = Instant.now();
        User user = userRepository.save(new User(fullName, phone, "not-a-real-hash", role, now));
        accountRepository.save(new Account(user, now));
        return new TestUser(user.getId(), tokenService.issue(user.getId(), role));
    }

    public record TestUser(UUID id, String token) {

        public String bearer() {
            return "Bearer " + token;
        }
    }
}