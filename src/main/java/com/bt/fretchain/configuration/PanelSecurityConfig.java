package com.bt.fretchain.configuration;

import com.bt.fretchain.security.PanelAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the role check for /admin/** and /partner/** APIs. */
@Configuration
@RequiredArgsConstructor
public class PanelSecurityConfig implements WebMvcConfigurer {

    private final PanelAuthorizationInterceptor panelAuthorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(panelAuthorizationInterceptor)
                .addPathPatterns("/admin/**", "/partner/**");
    }
}
