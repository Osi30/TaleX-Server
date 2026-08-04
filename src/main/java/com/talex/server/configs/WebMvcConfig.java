package com.talex.server.configs;

import com.talex.server.resolver.CurrentAccountIdArgumentResolver;
import com.talex.server.resolver.CurrentRoleArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final CurrentAccountIdArgumentResolver currentAccountIdArgumentResolver;
    private final CurrentRoleArgumentResolver currentRoleArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Register Resolvers
        resolvers.add(currentAccountIdArgumentResolver);
        resolvers.add(currentRoleArgumentResolver);
    }
}