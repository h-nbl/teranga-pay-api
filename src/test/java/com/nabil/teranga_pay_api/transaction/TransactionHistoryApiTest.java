package com.nabil.teranga_pay_api.transaction;

import com.nabil.teranga_pay_api.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionHistoryApiTest extends AbstractApiTest {

    private static final String HISTORY = "/api/transactions";
    private static final String DEPOSIT = "/api/accounts/me/deposit";

    @Test
    void historyListsMyTransactionsNewestFirstWithoutInternalFields() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");
        deposit(awa, 10_000);
        deposit(awa, 20_000);

        history(awa)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].amount").value(20000))
                .andExpect(jsonPath("$.items[1].amount").value(10000))
                .andExpect(jsonPath("$.items[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.items[0].direction").value("IN"))
                .andExpect(jsonPath("$.items[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[0].currency").value("XOF"))
                .andExpect(jsonPath("$.items[0].riskScore").doesNotExist())
                .andExpect(jsonPath("$.items[0].fromAccount").doesNotExist())
                .andExpect(jsonPath("$.items[0].toAccount").doesNotExist());
    }

    @Test
    void historyIsEmptyForAUserWithoutTransactions() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");

        history(awa)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void aUserNeverSeesAnotherUsersTransactions() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");
        TestUser moussa = createUser("Moussa Fall", "+221781234567");
        deposit(awa, 50_000);
        deposit(moussa, 5_000);

        history(awa)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].amount").value(50000));

        history(moussa)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].amount").value(5000));

        // Moussa essaie de cibler Awa : les paramètres inconnus sont ignorés
        history(moussa, "userId", awa.id().toString(), "accountId", awa.id().toString())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].amount").value(5000));
    }

    @Test
    void historyIsPaginated() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");
        deposit(awa, 1_000);
        deposit(awa, 2_000);
        deposit(awa, 3_000);

        history(awa, "size", "2", "page", "0")
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].amount").value(3000))
                .andExpect(jsonPath("$.items[1].amount").value(2000))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        history(awa, "size", "2", "page", "1")
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].amount").value(1000))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void historyCanBeFilteredByType() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");
        deposit(awa, 10_000);

        history(awa, "type", "DEPOSIT").andExpect(jsonPath("$.totalElements").value(1));
        history(awa, "type", "TRANSFER").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void historyCanBeFilteredByDate() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");
        deposit(awa, 10_000);
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);

        history(awa, "from", yesterday.toString(), "to", tomorrow.toString())
                .andExpect(jsonPath("$.totalElements").value(1));
        history(awa, "from", tomorrow.toString())
                .andExpect(jsonPath("$.totalElements").value(0));
        history(awa, "to", yesterday.toString())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidQueryParametersAreRefusedWith400() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);

        List<String[]> badRequests = List.of(
                new String[]{"size", "0"},
                new String[]{"size", "101"},
                new String[]{"page", "-1"},
                new String[]{"type", "NOPE"},
                new String[]{"from", "not-a-date"},
                new String[]{"from", tomorrow.toString(), "to", yesterday.toString()});

        for (String[] params : badRequests) {
            history(awa, params).andExpect(status().isBadRequest());
        }
    }

    @Test
    void historyWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(HISTORY)).andExpect(status().isUnauthorized());
    }

    private void deposit(TestUser user, long amount) throws Exception {
        mockMvc.perform(post(DEPOSIT)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + "}"))
                .andExpect(status().isCreated());
    }

    private ResultActions history(TestUser user, String... params) throws Exception {
        var request = get(HISTORY).header(HttpHeaders.AUTHORIZATION, user.bearer());
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }
}

