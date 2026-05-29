package org.example.rentathingproba.unit.infrastructure;

import io.jsonwebtoken.Claims;
import org.example.rentathingproba.service.infrastructure.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    // A 256-bit Base64-encoded secret (required by HS256)
    private static final String SECRET = "dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIVDI1Ng==";
    private static final Long EXPIRATION_MS = 3_600_000L;

    private UserDetails userDetails;

    @BeforeEach
    void setUp(){
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationTime", EXPIRATION_MS);

        userDetails = new User("test@example.com", "password", Collections.emptyList());
    }

    //Generate token / extract Username
    @Test
    @DisplayName("generateToken: produces a non-null, non-empty JWT string")
    void generateToken_producesNonEmptyToken(){
        String token = jwtService.generateToken(userDetails);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("extractUsername: returns the correct subject from a generated token")
    void extractUsername_returnsCorrectSubject(){
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
    }

    //Validate token
    @Test
    @DisplayName("validateToken: returns true for a freshly generated token with the same user")
    void validateToken_returnsTrueForValidToken() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.validateToken(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("validateToken: returns false when username in token does not match UserDetails")
    void validateToken_returnsFalseForDifferentUser() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = new User("other@example.com", "password", Collections.emptyList());
        assertThat(jwtService.validateToken(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for an already-expired token")
    void validateToken_returnsFalseForExpiredToken() {
        // Set a negative expiration so the token is expired immediately
        ReflectionTestUtils.setField(jwtService, "expirationTime", -1000L);
        String expiredToken = jwtService.generateToken(userDetails);

        // Restore expiration; the token itself is still expired
        ReflectionTestUtils.setField(jwtService, "expirationTime", EXPIRATION_MS);
        assertThat(jwtService.validateToken(expiredToken, userDetails)).isFalse();
    }

    //Get expiration time
    @Test
    @DisplayName("getExpirationTime: returns the configured expiration value")
    void getExpirationTime_returnsConfiguredValue() {
        assertThat(jwtService.getExpirationTime()).isEqualTo(EXPIRATION_MS);
    }

    //Extract claim
    @Test
    @DisplayName("extractClaim: can extract arbitrary claim (subject) via resolver")
    void extractClaim_extractsSubjectViaCustomResolver() {
        String token = jwtService.generateToken(userDetails);
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        assertThat(subject).isEqualTo("test@example.com");
    }
}
