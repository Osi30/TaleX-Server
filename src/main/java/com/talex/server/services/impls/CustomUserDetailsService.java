package com.talex.server.services.impls;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Account not found with username: " + username));

        return buildUserDetails(account);
    }

    public UserDetails loadByAccountId(UUID accountId) throws UsernameNotFoundException {
        log.debug("Loading user by accountId: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Account not found with id: " + accountId));

        return buildUserDetails(account);
    }

    private UserDetails buildUserDetails(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new UsernameNotFoundException(
                    "Account is not active: " + account.getEmail());
        }

        return User.builder()
                .username(account.getAccountId().toString())
                .password(account.getPassword() != null ? account.getPassword() : "")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_" + account.getRole().getCode())))
                .build();
    }
}
