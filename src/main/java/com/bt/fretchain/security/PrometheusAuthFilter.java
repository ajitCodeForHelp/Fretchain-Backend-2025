package com.bt.fretchain.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class PrometheusAuthFilter extends OncePerRequestFilter {


    private final PrometheusAuthenticationProvider prometheusAuthenticationProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (canSkipAuthentication(request)) {
            chain.doFilter(request, response);
            return;
        }

        final String requestTokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = requestTokenHeader;
        if (requestTokenHeader != null && requestTokenHeader.startsWith("ha-monitor-token")) {
            token = requestTokenHeader.substring(17);
        }

        if (token == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        // log.info("Token {} ", token);

        var auth = prometheusAuthenticationProvider.authenticate(new PrometheusAuthentication(token));
        if (auth == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    private boolean canSkipAuthentication(HttpServletRequest request) {
        List<RequestMatcher> matchers = new ArrayList<>();

        for (String pattern : Constants.IGNORE_PATTERNS) {
            matchers.add(new AntPathRequestMatcher(pattern));
        }

        for (String pattern : Constants.PERMIT_ALL_PATTERNS) {
            matchers.add(new AntPathRequestMatcher(pattern));
        }

        for (String pattern : Constants.PROTECTED_PATTERNS) {
            matchers.add(new AntPathRequestMatcher(pattern));
        }

        RequestMatcher ignoreRequestMatcher = new OrRequestMatcher(matchers);
        return ignoreRequestMatcher.matches(request);
    }
}
