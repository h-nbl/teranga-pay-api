package com.nabil.teranga_pay_api.auth;

import com.nabil.teranga_pay_api.TestcontainersConfiguration;
import com.nabil.teranga_pay_api.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityRulesTest {

    private static final String ANY_ROUTE = "/api/this-route-does-not-exist";
    private static final String ADMIN_ROUTE = "/api/admin/this-route-does-not-exist";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void requestWithoutTokenIsRejectedWith401() throws Exception {
        mockMvc.perform(get(ANY_ROUTE))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Authentification requise"));
    }

    @Test
    void validUserTokenIsAccepted() throws Exception {
        mockMvc.perform(get(ANY_ROUTE).header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(Role.USER))))
                .andExpect(status().isNotFound());
    }

    @Test
    void userTokenOnAdminRouteIsForbidden() throws Exception {
        mockMvc.perform(get(ADMIN_ROUTE).header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(Role.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Accès refusé"));
    }

    @Test
    void adminTokenOnAdminRoutePassesAuthorization() throws Exception {
        mockMvc.perform(get(ADMIN_ROUTE).header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(Role.ADMIN))))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenWithInvalidSignatureIsRejected() throws Exception {
        String[] parts = tokenFor(Role.USER).split("\\.");
        String badSignature = "A".repeat(parts[2].length());

        expectUnauthorized(parts[0] + "." + parts[1] + "." + badSignature);
    }

    @Test
    void tokenWithForgedRoleClaimIsRejected() throws Exception {
        String[] parts = tokenFor(Role.USER).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), UTF_8);
        assertTrue(payload.contains("\"role\":\"USER\""), "le garde-fou du test a échoué");

        String forgedPayload = payload.replace("\"role\":\"USER\"", "\"role\":\"ADMIN\"");
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(forgedPayload.getBytes(UTF_8));

        expectUnauthorized(parts[0] + "." + encoded + "." + parts[2]);
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        String expired = new TokenService(jwtEncoder, jwtProperties, twoHoursAgo)
                .issue(UUID.randomUUID(), Role.USER);

        expectUnauthorized(expired);
    }

    @Test
    void unsignedTokenWithAlgNoneIsRejected() throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(UTF_8));
        long exp = Instant.now().plusSeconds(1800).getEpochSecond();
        String payload = encoder.encodeToString((
                "{\"iss\":\"" + JwtConfig.ISSUER + "\",\"sub\":\"" + UUID.randomUUID()
                        + "\",\"role\":\"ADMIN\",\"exp\":" + exp + "}").getBytes(UTF_8));

        expectUnauthorized(header + "." + payload + ".");
    }

    private void expectUnauthorized(String token) throws Exception {
        mockMvc.perform(get(ADMIN_ROUTE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentification requise"));
    }

    private String tokenFor(Role role) {
        return tokenService.issue(UUID.randomUUID(), role);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}