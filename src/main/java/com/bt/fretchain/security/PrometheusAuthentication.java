package com.bt.fretchain.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class PrometheusAuthentication implements Authentication {

    private static final long serialVersionUID = -1963784332267631995L;

    private String tokenValue;
    private boolean authenticated;

    public PrometheusAuthentication(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return tokenValue;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    @Override
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    public String getToken() {
        return tokenValue;
    }
}
