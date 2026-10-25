package com.nabil.teranga_pay_api.auth;

import com.nabil.teranga_pay_api.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {

    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtProperties properties =
            new JwtProperties("test-secret-test-secret-test-secret-0123", 30);
    private final JwtDecoder decoder = jwtConfig.jwtDecoder(properties);

    private TokenService serviceWith(JwtProperties props, Clock clock) {
        return new TokenService(jwtConfig.jwtEncoder(props), props, clock);
    }

    @Test
    void issuedTokenCarriesUserIdRoleAndThirtyMinutesLifetime() {
        UUID userId = UUID.randomUUID();

        String token = serviceWith(properties, Clock.systemUTC()).issue(userId, Role.ADMIN);
        Jwt jwt = decoder.decode(token);

        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals("ADMIN", jwt.getClaimAsString("role"));
        assertEquals(Duration.ofMinutes(30),
                Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtProperties other =
                new JwtProperties("another-secret-another-secret-another-0123", 30);
        String token = serviceWith(other, Clock.systemUTC()).issue(UUID.randomUUID(), Role.USER);

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void expiredTokenIsRejected() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        String token = serviceWith(properties, twoHoursAgo).issue(UUID.randomUUID(), Role.USER);

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }
}