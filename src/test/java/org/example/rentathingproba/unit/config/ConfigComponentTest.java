package org.example.rentathingproba.unit.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.example.rentathingproba.config.JwtAuthFilter;
import org.example.rentathingproba.config.LoggingInterceptor;
import org.example.rentathingproba.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Config Component Unit Tests")
class ConfigComponentTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HandlerExceptionResolver handlerExceptionResolver;

    private JwtAuthFilter jwtAuthFilter;
    private LoggingInterceptor loggingInterceptor;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter(jwtService, userDetailsService, handlerExceptionResolver);
        loggingInterceptor = new LoggingInterceptor();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("LoggingInterceptor.preHandle: always returns true")
    void loggingInterceptor_preHandle_returnsTrue() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/listings");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = loggingInterceptor.preHandle(req, res, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("LoggingInterceptor.afterCompletion: does not throw when no exception")
    void loggingInterceptor_afterCompletion_noException() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/things");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setStatus(200);

        loggingInterceptor.afterCompletion(req, res, new Object(), null);
    }

    @Test
    @DisplayName("LoggingInterceptor.afterCompletion: does not throw when exception is present")
    void loggingInterceptor_afterCompletion_withException() {
        MockHttpServletRequest req = new MockHttpServletRequest("DELETE", "/things/1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        loggingInterceptor.afterCompletion(req, res, new Object(), new RuntimeException("test error"));
    }

    @Test
    @DisplayName("JwtAuthFilter: skips JWT processing for /auth/ paths and forwards the request")
    void jwtAuthFilter_skipsAuthPaths() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
        req.setServletPath("/auth/login");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        // Chain was invoked, so the filter was skipped
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("JwtAuthFilter: passes through when Authorization header is absent")
    void jwtAuthFilter_noAuthHeader_passesThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/listings");
        req.setServletPath("/listings");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("JwtAuthFilter: passes through when Authorization header is not Bearer")
    void jwtAuthFilter_nonBearerHeader_passesThrough() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/listings");
        req.setServletPath("/listings");
        req.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        jwtAuthFilter.doFilter(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("JwtAuthFilter: sets SecurityContext authentication when token is valid")
    void jwtAuthFilter_validToken_setsAuthentication() throws Exception {
        String token = "valid.jwt.token";
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "user@test.com", "pass", Collections.emptyList());

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/listings");
        req.setServletPath("/listings");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUsername(token)).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.validateToken(token, userDetails)).thenReturn(true);

        jwtAuthFilter.doFilter(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("JwtAuthFilter: does not set authentication when token validation fails")
    void jwtAuthFilter_invalidToken_doesNotSetAuthentication() throws Exception {
        String token = "bad.jwt.token";
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "user@test.com", "pass", Collections.emptyList());

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/listings");
        req.setServletPath("/listings");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUsername(token)).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.validateToken(token, userDetails)).thenReturn(false);

        jwtAuthFilter.doFilter(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("JwtAuthFilter: returns 401 JSON body when token is expired")
    void jwtAuthFilter_expiredToken_returns401() throws Exception {
        String token = "expired.jwt.token";

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/listings");
        req.setServletPath("/listings");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUsername(token))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        jwtAuthFilter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(res.getContentAsString()).contains("expired");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("JwtAuthFilter: delegates to HandlerExceptionResolver on JwtException")
    void jwtAuthFilter_jwtException_delegatesToResolver() throws Exception {
        String token = "malformed.jwt.token";

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/listings");
        req.setServletPath("/listings");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUsername(token)).thenThrow(new JwtException("invalid"));

        jwtAuthFilter.doFilter(req, res, chain);

        verify(handlerExceptionResolver).resolveException(eq(req), eq(res), isNull(), any(JwtException.class));
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("JwtAuthFilter: delegates to HandlerExceptionResolver on unexpected exception")
    void jwtAuthFilter_unexpectedException_delegatesToResolver() throws Exception {
        String token = "some.jwt.token";

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/listings");
        req.setServletPath("/listings");
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("unexpected"));

        jwtAuthFilter.doFilter(req, res, chain);

        verify(handlerExceptionResolver).resolveException(eq(req), eq(res), isNull(), any(RuntimeException.class));
        assertThat(chain.getRequest()).isNull();
    }
}