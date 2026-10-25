package com.nabil.teranga_pay_api.auth;

import com.jayway.jsonpath.JsonPath;
import com.nabil.teranga_pay_api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AuthApiTest {

    private static final String REGISTER = "/api/auth/register";
    private static final String LOGIN = "/api/auth/login";
    private static final String ANY_ROUTE = "/api/this-route-does-not-exist";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerReturns201WithNormalizedPhoneAndNoSecrets() throws Exception {
        MvcResult result = postJson(REGISTER, registerJson("Awa Diop", "77 123 45 67", "motdepasse123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.fullName").value("Awa Diop"))
                .andExpect(jsonPath("$.phone").value("+221771234567"))
                .andReturn();

        String body = result.getResponse().getContentAsString().toLowerCase();
        assertFalse(body.contains("motdepasse123"));
        assertFalse(body.contains("$2"));
        assertFalse(body.contains("hash"));
        assertFalse(body.contains("password"));
    }

    @Test
    void loginReturnsBearerTokenThatOpensProtectedRoutes() throws Exception {
        postJson(REGISTER, registerJson("Awa Diop", "+221771234567", "motdepasse123"))
                .andExpect(status().isCreated());

        MvcResult login = postJson(LOGIN, loginJson("77 123 45 67", "motdepasse123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andReturn();

        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get(ANY_ROUTE).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void registeringTheSameNumberTwiceReturns409() throws Exception {
        postJson(REGISTER, registerJson("Awa Diop", "+221771234567", "motdepasse123"))
                .andExpect(status().isCreated());

        postJson(REGISTER, registerJson("Autre Personne", "77 123 45 67", "autremotdepasse1"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void wrongPasswordAndUnknownNumberGiveIdenticalResponses() throws Exception {
        postJson(REGISTER, registerJson("Awa Diop", "+221771234567", "motdepasse123"))
                .andExpect(status().isCreated());

        MvcResult wrongPassword = postJson(LOGIN, loginJson("+221771234567", "mauvais-mot-de-passe"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult unknownNumber = postJson(LOGIN, loginJson("+221781234567", "motdepasse123"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertEquals(wrongPassword.getResponse().getContentAsString(),
                unknownNumber.getResponse().getContentAsString());
    }

    @Test
    void invalidBodyReturns400ListingFieldsButNotTheRejectedValues() throws Exception {
        MvcResult result = postJson(REGISTER, registerJson("", "77 123 45 67", "Zq7Wk2p"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field=='fullName')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").exists())
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains("Zq7Wk2p"));
    }

    @Test
    void invalidPhoneReturns400() throws Exception {
        postJson(REGISTER, registerJson("Awa Diop", "abc", "motdepasse123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Numéro de téléphone invalide"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        postJson(REGISTER, "{\"fullName\":")
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOnLoginReturns405WithAllowHeader() throws Exception {
        mockMvc.perform(get(LOGIN))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, "POST"));
    }

    @Test
    void rejectedRegistrationPasswordIsNeverLogged(CapturedOutput output) throws Exception {
        postJson(REGISTER, registerJson("Awa Diop", "77 123 45 67", "Zq7Wk2p"))
                .andExpect(status().isBadRequest());

        assertFalse(output.getAll().contains("Zq7Wk2p"));
    }

    @Test
    void wrongLoginPasswordIsNeverLogged(CapturedOutput output) throws Exception {
        postJson(LOGIN, loginJson("+221771234567", "Wr0ngPassw0rd-Unique"))
                .andExpect(status().isUnauthorized());

        assertFalse(output.getAll().contains("Wr0ngPassw0rd-Unique"));
    }

    @Test
    void failedLoginsAreLoggedWithReasonAndMaskedPhone(CapturedOutput output) throws Exception {
        postJson(REGISTER, registerJson("Awa Diop", "+221771234567", "motdepasse123"))
                .andExpect(status().isCreated());

        postJson(LOGIN, loginJson("77 123 45 67", "mauvais-mot-de-passe"))
                .andExpect(status().isUnauthorized());
        postJson(LOGIN, loginJson("+221781234599", "motdepasse123"))
                .andExpect(status().isUnauthorized());

        String logs = output.getAll();
        assertTrue(logs.contains("Login failed: reason=bad_password phone=***67"));
        assertTrue(logs.contains("Login failed: reason=unknown_number phone=***99"));
        assertFalse(logs.contains("771234567"));
        assertFalse(logs.contains("781234599"));
        assertFalse(logs.contains("mauvais-mot-de-passe"));
    }

    @Test
    void successfulLoginIsLoggedWithUserIdButNoPhone(CapturedOutput output) throws Exception {
        MvcResult registered = postJson(REGISTER, registerJson("Awa Diop", "+221771234567", "motdepasse123"))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = JsonPath.read(registered.getResponse().getContentAsString(), "$.userId");

        postJson(LOGIN, loginJson("+221771234567", "motdepasse123"))
                .andExpect(status().isOk());

        assertTrue(output.getAll().contains("Login succeeded: userId=" + userId));
        assertFalse(output.getAll().contains("771234567"));
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String registerJson(String fullName, String phone, String password) {
        return "{\"fullName\":\"%s\",\"phone\":\"%s\",\"password\":\"%s\"}"
                .formatted(fullName, phone, password);
    }

    private static String loginJson(String phone, String password) {
        return "{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(phone, password);
    }
}