package org.example.rentathingproba.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.rentathingproba.service.infrastructure.JwtService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService, HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    // ✅ ISPRAVLJENO: Koristimo getServletPath() umjesto getRequestURI()
    // getRequestURI() vraća /api/auth/... (uključuje context-path)
    // getServletPath() vraća /auth/... (bez context-patha)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // ✅ DEBUG LOGGING
        log.debug("[JWT_FILTER] Request: {} {}", request.getMethod(), request.getServletPath());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JWT_FILTER] No Bearer token found, skipping filter");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        log.debug("[JWT_FILTER] Token received: {}...", jwt.substring(0, Math.min(30, jwt.length())));

        try {
            final String userEmail = jwtService.extractUsername(jwt);
            log.debug("[JWT_FILTER] Extracted email: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                log.debug("[JWT_FILTER] User loaded: {}", userDetails.getUsername());

                if (jwtService.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("[JWT_FILTER] ✅ Authentication SET for user: {}", userEmail);
                } else {
                    log.warn("[JWT_FILTER] ❌ Token validation FAILED for user: {}", userEmail);
                }
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            log.warn("[JWT_FILTER] ❌ Expired JWT: {}", ex.getMessage());
            // ✅ VAŽNO: Ne nastavljamo filter chain, vraćamo 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Token expired. Please login again.\"}");
        } catch (JwtException ex) {
            log.warn("[JWT_FILTER] ❌ Invalid JWT: {}", ex.getMessage());
            handlerExceptionResolver.resolveException(request, response, null, ex);
        } catch (Exception e) {
            log.error("[JWT_FILTER] ❌ Unexpected error: {}", e.getMessage(), e);
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}