package com.talex.server.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Swagger UI + API docs — cho phép truy cập không cần login
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs/**"
                ).permitAll()

                // Tất cả request khác — tạm thời cho phép hết (sẽ lock down sau)
                .anyRequest().permitAll()
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
