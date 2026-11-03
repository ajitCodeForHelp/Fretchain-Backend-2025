package com.bt.fretchain.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the Bearer JWT and puts a {@link JwtAuthenticationToken} in the security context.
 *
 * - Protected paths: a valid token is required, otherwise 401.
 * - Public paths: the token is optional; when a valid one is sent the user is still
 *   identified, so JwtUserDetailsService.findLoggedInUser() works there too.
 *
 * A token is valid when its signature and expiry are correct (checked while parsing)
 * and its owner ("u-id") still exists. The owner's name is NOT compared any more, so
 * renaming a user no longer invalidates their login.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getMethod().equals("OPTIONS") || isIgnored(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        boolean publicPath = isPublic(request);

        boolean authenticated = authenticate(request.getHeader(HttpHeaders.AUTHORIZATION));

        if (!authenticated && !publicPath) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** @return true when the header carries a valid token and the context now holds it. */
    private boolean authenticate(String header) {
        if (header == null || header.isBlank()) {
            return false;
        }
        String jwtToken = header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
        if (jwtToken.isEmpty() || "null".equals(jwtToken) || "undefined".equals(jwtToken)) {
            return false;
        }

        String username;
        String userId;
        String userType;
        String deviceId;
        String userToken;
        try {
            // parsing verifies the signature and the expiry
            username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            userId = (String) jwtTokenUtil.getValueFromClaimsKey(jwtToken, "u-id");
            userType = (String) jwtTokenUtil.getValueFromClaimsKey(jwtToken, "u-ty");
            deviceId = (String) jwtTokenUtil.getValueFromClaimsKey(jwtToken, "d-id");
            userToken = (String) jwtTokenUtil.getValueFromClaimsKey(jwtToken, "token");
        } catch (Exception e) {
            logger.debug("Invalid JWT: " + e.getMessage());
            return false;
        }

        if (!jwtUserDetailsService.tokenOwnerExists(userId, userType)) {
            return false;
        }

        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken)) {
            Jwt.Builder jwt = Jwt.withTokenValue(jwtToken)
                    .claim("u-id", userId)
                    .header(HttpHeaders.AUTHORIZATION, header);
            // Jwt does not accept null claim values
            if (username != null) jwt.claim("username", username);
            if (userType != null) jwt.claim("u-ty", userType);
            if (deviceId != null) jwt.claim("d-id", deviceId);
            if (userToken != null) jwt.claim("token", userToken);

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt.build());
            authentication.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        return true;
    }

    private static final RequestMatcher IGNORED = matcher(Constants.IGNORE_PATTERNS);
    private static final RequestMatcher PUBLIC = matcher(Constants.PERMIT_ALL_PATTERNS);

    private boolean isIgnored(HttpServletRequest request) {
        return IGNORED.matches(request);
    }

    private boolean isPublic(HttpServletRequest request) {
        return PUBLIC.matches(request);
    }

    private static RequestMatcher matcher(String[] patterns) {
        List<RequestMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            matchers.add(new AntPathRequestMatcher(pattern));
        }
        return new OrRequestMatcher(matchers);
    }
}
