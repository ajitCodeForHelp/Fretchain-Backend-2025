package com.bt.fretchain.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class PrometheusAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PrometheusAuthentication prometheusAuthentication = (PrometheusAuthentication) authentication;
        BearerTokenAuthenticationToken bearerTokenAuthenticationToken = new BearerTokenAuthenticationToken(prometheusAuthentication.getToken());
        bearerTokenAuthenticationToken.setAuthenticated(true);
        return prometheusAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.getName().equals("com.bt.cloudLibrary.project.security.PrometheusAuthentication");
    }
}
