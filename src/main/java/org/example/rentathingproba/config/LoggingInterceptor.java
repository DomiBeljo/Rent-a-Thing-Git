package org.example.rentathingproba.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class LoggingInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("Incoming request: method={}, uri={}, remoteAddr{}" ,request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null) {
            log.error("Request completed with exception: method={}, uri={}, error={}",
                    request.getMethod(), request.getRequestURI(), ex.getMessage());
        } else{
            log.info("Request completed: method={}, uri={}, status={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }


}
