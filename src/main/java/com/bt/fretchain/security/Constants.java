package com.bt.fretchain.security;

public class Constants {

    public static final String[] IGNORE_PATTERNS = new String[]{"**/*.js", "**/*.css", "/resources/**", "/images/**", "/css/**", "/js/**"};
    public static final String[] PERMIT_ALL_PATTERNS = new String[]{
            "/auth/**",
            "/public/**",
                "/guest/**",
            "/guest-customer/**",
            "/",

            "/swagger-ui/index.html",
            "/swagger*/**",
            "/*/api-docs/**",
            "/api-docs",
            "/api-docs/**",

            "/.well-known/**",
            "/apple-app-site-association",

            "/website/**",
            "/*/api/guest/**",
            "/webjars/springfox-swagger-ui/**",
            "/csrf",
            "/v1/**",
            "/pages/**",
            "/onboarding/**",
            "/contactUsOrQuoteRequestMail/**",
            "/kafka/**",
            "/actuator/**",
            "/customer/api/auth/**",
            "/customer/api/home/**",
            "/admin/api/auth/**"
    };
    public static final String[] MONITORING_PATTERNS = new String[]{"/actuator1/**"};
    public static final String[] PROTECTED_PATTERNS = new String[]{"/admin/**", "/partner/**", "/customer/**", "/ops/**"};
}
