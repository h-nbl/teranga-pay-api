package com.nabil.teranga_pay_api.account;

import com.nabil.teranga_pay_api.AbstractApiTest;
import com.nabil.teranga_pay_api.transaction.Transaction;
import com.nabil.teranga_pay_api.transaction.TransactionRepository;
import com.nabil.teranga_pay_api.transaction.TransactionStatus;
import com.nabil.teranga_pay_api.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepositApiTest extends AbstractApiTest {

    private static final String DEPOSIT = "/api/accounts/me/deposit";
    private static final String ME = "/api/accounts/me";

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletProperties walletProperties;

    @Test
    void depositIncreasesBalanceAndRecordsACompletedDepositTransaction() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");

        deposit(awa, 50_000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.balance").value(50000))
                .andExpect(jsonPath("$.currency").value("XOF"));

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, awa.bearer()))
                .andExpect(jsonPath("$.balance").value(50000));

        List<Transaction> recorded = transactionsOf(accountOf(awa));
        assertEquals(1, recorded.size());
        Transaction deposit = recorded.get(0);
        assertEquals(TransactionType.DEPOSIT, deposit.getType());
        assertEquals(TransactionStatus.COMPLETED, deposit.getStatus());
        assertEquals(50_000, deposit.getAmount());
        assertNull(deposit.getFromAccount());
        assertEquals(0, deposit.getRiskScore());
    }

    @Test
    void depositAboveTheLimitIsRefusedWith422AndNothingIsRecorded() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");

        deposit(awa, walletProperties.maxDeposit() + 1)
                .andExpect(status().is(422))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        assertEquals(0, accountOf(awa).getBalance());
        assertTrue(transactionsOf(accountOf(awa)).isEmpty());
    }

    @Test
    void depositExactlyAtTheLimitIsAccepted() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");

        deposit(awa, walletProperties.maxDeposit())
                .andExpect(status().isCreated());
    }

    @Test
    void zeroNegativeOrMissingAmountIsRefusedWith400() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");

        for (String body : List.of("{\"amount\":0}", "{\"amount\":-5000}", "{}")) {
            postDeposit(awa, body).andExpect(status().isBadRequest());
        }

        assertEquals(0, accountOf(awa).getBalance());
    }

    @Test
    void depositWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post(DEPOSIT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void depositCreditsOnlyTheAuthenticatedUserEvenIfTheBodyNamesSomeoneElse() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");
        TestUser moussa = createUser("Moussa Fall", "+221781234567");

        postDeposit(awa, "{\"amount\":10000,\"userId\":\"%s\",\"accountId\":\"%s\"}"
                .formatted(moussa.id(), moussa.id()))
                .andExpect(status().isCreated());

        assertEquals(10_000, accountOf(awa).getBalance());
        assertEquals(0, accountOf(moussa).getBalance());
    }

    @Test
    void successiveDepositsAccumulate() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");

        deposit(awa, 10_000).andExpect(status().isCreated());
        deposit(awa, 20_000)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(30000));

        assertEquals(2, transactionsOf(accountOf(awa)).size());
    }

    private ResultActions deposit(TestUser user, long amount) throws Exception {
        return postDeposit(user, "{\"amount\":" + amount + "}");
    }

    private ResultActions postDeposit(TestUser user, String json) throws Exception {
        return mockMvc.perform(post(DEPOSIT)
                .header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Account accountOf(TestUser user) {
        return accountRepository.findByUserId(user.id()).orElseThrow();
    }

    private List<Transaction> transactionsOf(Account account) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getToAccount().getId().equals(account.getId()))
                .toList();
    }
}