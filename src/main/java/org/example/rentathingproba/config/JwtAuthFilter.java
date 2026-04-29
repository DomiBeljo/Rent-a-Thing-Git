package org.example.rentathingproba.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.rentathingproba.service.infrastructure.JwtService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.info("JWT FILTER >>> {} {}", method, uri);

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("JWT FILTER >>> No Bearer token for {} {} — continuing without auth", method, uri);
            filterChain.doFilter(request, response);
            return;
        }

        log.info("JWT FILTER >>> Token found, validating...");

        try {
            final String jwt = authorizationHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);
            log.info("JWT FILTER >>> Token email: {}", userEmail);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (userEmail != null && authentication == null) {
                UserDetails userDetails;
                try {
                    userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                    log.info("JWT FILTER >>> User found in DB: {}", userEmail);
                } catch (Exception e) {
                    log.error("JWT FILTER >>> User NOT found in DB for email: {} — stale token, skipping auth", userEmail);
                    filterChain.doFilter(request, response);
                    return;
                }

                boolean valid = jwtService.validateToken(jwt, userDetails);
                log.info("JWT FILTER >>> Token valid: {}", valid);

                if (valid) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("JWT FILTER >>> Authentication set for: {}", userEmail);
                } else {
                    log.warn("JWT FILTER >>> Token failed validation for: {}", userEmail);
                }
            } else {
                log.info("JWT FILTER >>> Already authenticated or no email in token");
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT FILTER >>> Exception: {}", e.getMessage());
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}