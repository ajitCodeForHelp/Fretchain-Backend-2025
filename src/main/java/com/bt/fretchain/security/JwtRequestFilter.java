package com.bt.fretchain.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (request.getMethod().equals("OPTIONS") || canSkipAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        final String requestTokenHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        String userId = null;
        String userType = null;
        String deviceId = null;
        String userToken = null;
        String username = null;

        String jwtToken = null;
        if (requestTokenHeader != null) {
            if (requestTokenHeader.startsWith("Bearer ")) {
                jwtToken = requestTokenHeader.substring(7);
            } else {
                jwtToken = requestTokenHeader;
            }
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                userId = (String) jwtTokenUtil.getValueFromClaimsKey(jwtToken, "u-id");
                userType = (String) jwtTokenUtil.getValueFromClaimsKey(jwtToken, "u-ty");
                deviceId = (String) jwtTokenUtil.getValueFromClaimsKey(jwtToken, "d-id");
                userToken = (String) jwtTokenUtil.getValueFromClaimsKey(jwtToken, "token");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }


        // TODO  >> Need to unComment It

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.jwtUserDetailsService.loadUserByUsername(userId, userType, userToken);
            // UserAdmin userAdmin = this.jwtUserDetailsService.findUserAdmin(username);
            if (Boolean.TRUE.equals(jwtTokenUtil.validateToken(jwtToken, userDetails))) {
                JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(
                        Jwt.withTokenValue(jwtToken)
                                .claim("username", username)
                                .claim("u-id", userId)
                                .claim("u-ty", userType)
                                .claim("d-id", deviceId)
                                .claim("token", userToken)
                                .header(HttpHeaders.AUTHORIZATION, requestTokenHeader)
                                .build()
                );
                jwtAuthenticationToken.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean canSkipAuthentication(HttpServletRequest request) {
        // if (true) return true;
        List<RequestMatcher> matchers = new ArrayList<>();
        for (String pattern : Constants.IGNORE_PATTERNS) {
            matchers.add(new AntPathRequestMatcher(pattern));
        }
        for (String pattern : Constants.PERMIT_ALL_PATTERNS) {
            matchers.add(new AntPathRequestMatcher(pattern));
        }
        RequestMatcher ignoreRequestMatcher = new OrRequestMatcher(matchers);
        return ignoreRequestMatcher.matches(request);
    }
}
