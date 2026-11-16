package com.bt.fretchain.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Writes one WARN line for every request that takes longer than SLOW_MS inside the backend, e.g.
 * "SLOW 2310 ms POST /customer/api/store-order/list -> 200". The time covers the whole request (security,
 * database, response), not the trip over the internet. Look for it with:
 * sudo journalctl -u fretchain.service | grep SLOW
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SlowRequestLogFilter extends OncePerRequestFilter {

    private static final long SLOW_MS = 300;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            if (ms >= SLOW_MS && !request.getRequestURI().startsWith("/actuator")) {
                log.warn("SLOW {} ms {} {} -> {}", ms, request.getMethod(), request.getRequestURI(), response.getStatus());
            }
        }
    }
}
