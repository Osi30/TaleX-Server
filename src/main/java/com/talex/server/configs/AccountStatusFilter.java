package com.talex.server.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.responses.ApiResponse;
import com.talex.server.entities.Account;
import com.talex.server.enums.AccountStatus;
import com.talex.server.repositories.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountStatusFilter extends OncePerRequestFilter {

    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;

    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || request.getRequestURI().startsWith(AUTH_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID accountId;
        try {
            accountId = UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            filterChain.doFilter(request, response);
            return;
        }

        Account account = accountRepository.findById(accountId).orElse(null);
        if (account != null && account.getStatus() == AccountStatus.INCOMPLETE) {
            log.warn("INCOMPLETE account {} attempted access to {}",
                    accountId, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error("Please complete your registration"));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
