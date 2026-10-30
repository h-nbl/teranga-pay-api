package com.nabil.teranga_pay_api.account;

import com.nabil.teranga_pay_api.AbstractApiTest;
import com.nabil.teranga_pay_api.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountApiTest extends AbstractApiTest {

    private static final String ME = "/api/accounts/me";

    @Test
    void meReturnsNameNumberBalanceAndCurrencyOfTheAuthenticatedUser() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, awa.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Awa Diop"))
                .andExpect(jsonPath("$.phone").value("+221771234567"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.currency").value("XOF"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void eachUserSeesOnlyTheirOwnAccount() throws Exception {
        createUser("Awa Diop", "+221771234567");
        TestUser moussa = createUser("Moussa Fall", "+221781234567");

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, moussa.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Moussa Fall"))
                .andExpect(jsonPath("$.phone").value("+221781234567"));
    }

    @Test
    void userIdInParameterOrHeaderIsIgnored() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");
        TestUser moussa = createUser("Moussa Fall", "+221781234567");

        mockMvc.perform(get(ME)
                        .param("userId", moussa.id().toString())
                        .param("id", moussa.id().toString())
                        .header("X-User-Id", moussa.id().toString())
                        .header(HttpHeaders.AUTHORIZATION, awa.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Awa Diop"));
    }

    @Test
    void balanceComesFromTheDatabase() throws Exception {
        TestUser awa = createUser("Awa Diop", "+221771234567");
        Account account = accountRepository.findByUserId(awa.id()).orElseThrow();
        account.credit(25_000);
        accountRepository.saveAndFlush(account);

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, awa.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(25000));
    }

    @Test
    void validTokenWithoutAccountReturns404() throws Exception {
        String token = tokenService.issue(UUID.randomUUID(), Role.USER);

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Compte introuvable"));
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(get(ME))
                .andExpect(status().isUnauthorized());
    }
}