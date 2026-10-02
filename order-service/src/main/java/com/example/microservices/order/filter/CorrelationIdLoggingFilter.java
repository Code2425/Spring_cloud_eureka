package com.example.microservices.order.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CorrelationIdLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put("correlationId", correlationId);
        }
        try {
            log.info("Incoming request {} {} correlationId={}", request.getMethod(), request.getRequestURI(), correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
