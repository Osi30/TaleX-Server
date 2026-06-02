package com.talex.server.services;

import com.talex.server.entities.Account;
import com.talex.server.enums.AccountStatus;
import com.talex.server.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user: {}", email);

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Account not found with email: " + email));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new UsernameNotFoundException(
                    "Account is not active: " + email);
        }

        return User.builder()
                .username(account.getEmail())
                .password(account.getPassword() != null ? account.getPassword() : "")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_" + account.getRole().getCode())))
                .build();
    }
}
