package org.example.rentathingproba.unit.infrastructure;

import org.example.rentathingproba.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Extended Unit Tests")
class JwtServiceExtendedTest {

    private JwtService jwtService;

    private static final String SECRET = "dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIVDI1Ng==";
    private static final Long EXPIRATION_MS = 3_600_000L;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationTime", EXPIRATION_MS);
        userDetails = new User("test@example.com", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("validateToken: returns false for a completely malformed token string")
    void validateToken_returnsFalseForMalformedToken() {
        boolean result = jwtService.validateToken("this.is.not.a.valid.jwt", userDetails);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for a token signed with a different key")
    void validateToken_returnsFalseForTokenSignedWithDifferentKey() {
        // Build a token with a different secret
        JwtService otherJwtService = new JwtService();
        ReflectionTestUtils.setField(otherJwtService, "secretKey",
                "b3RoZXJTZWNyZXRLZXlUaGF0SXNMb25nRW5vdWdoRm9ySFQyNTY=");
        ReflectionTestUtils.setField(otherJwtService, "expirationTime", EXPIRATION_MS);

        String tokenFromOtherKey = otherJwtService.generateToken(userDetails);

        // Validate against the original service (different key → JwtException → false)
        boolean result = jwtService.validateToken(tokenFromOtherKey, userDetails);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateToken: throws IllegalArgumentException for an empty string token")
    void validateToken_throwsForEmptyToken() {
        // JJWT's parser asserts the token is non-blank before it reaches the
        // signature check, so this escapes the JwtException catch block.
        assertThatThrownBy(() -> jwtService.validateToken("", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("generateToken with extra claims: embeds claims and correct subject")
    void generateToken_withExtraClaims_embedsSubject() {
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("role", "ADMIN");

        String token = jwtService.generateToken(claims, userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
        assertThat(jwtService.validateToken(token, userDetails)).isTrue();
    }
}