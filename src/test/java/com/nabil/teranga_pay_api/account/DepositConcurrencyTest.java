package com.nabil.teranga_pay_api.account;

import com.nabil.teranga_pay_api.TestcontainersConfiguration;
import com.nabil.teranga_pay_api.auth.TokenService;
import com.nabil.teranga_pay_api.transaction.TransactionRepository;
import com.nabil.teranga_pay_api.user.Role;
import com.nabil.teranga_pay_api.user.User;
import com.nabil.teranga_pay_api.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; // ADAPTER 1 : même import que dans AbstractApiTest
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DepositConcurrencyTest {

    private static final int THREADS = 20;
    private static final long AMOUNT = 1_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TokenService tokenService;

    @AfterEach
    void cleanUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void concurrentDepositsAreAllAppliedAndNoneIsLost() throws Exception {
        // ADAPTER 2 : construis User et Account comme le fait UserPersistenceTest
        User user = userRepository.save(
                new User("Awa Diop", "+221771234567", "not-a-real-hash", Role.USER, Instant.now()));
        Account account = accountRepository.save(new Account(user, Instant.now()));
        // ADAPTER 3 : obtiens le jeton comme le fait AbstractApiTest.createUser
        String token = tokenService.issue(user.getId(), Role.USER);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            statuses.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return mockMvc.perform(post("/api/accounts/me/deposit")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"amount\":" + AMOUNT + "}"))
                        .andReturn().getResponse().getStatus();
            }));
        }

        ready.await();
        go.countDown();

        try {
            for (Future<Integer> status : statuses) {
                assertEquals(201, status.get(60, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        Account reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(THREADS * AMOUNT, reloaded.getBalance());
        assertEquals(THREADS, transactionRepository.count());
    }
}