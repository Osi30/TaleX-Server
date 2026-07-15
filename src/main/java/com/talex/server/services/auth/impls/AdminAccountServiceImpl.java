package com.talex.server.services.auth.impls;

import com.talex.server.dtos.requests.auth.CreateStaffRequestDto;
import com.talex.server.dtos.responses.auth.AdminAccountResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.enums.AccountStatus;
import com.talex.server.exceptions.codes.AdminAccountErrorCode;
import com.talex.server.exceptions.details.AdminAccountException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.services.auth.AdminAccountService;
import com.talex.server.services.auth.IRoleService;
import com.talex.server.specifications.AccountSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAccountServiceImpl implements AdminAccountService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String STAFF_ROLE = "STAFF";

    private final AccountRepository accountRepository;
    private final IRoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminAccountResponseDto> getAccounts(String keyword, String roleName, String status, Pageable pageable) {
        return accountRepository.findAll(new AccountSpecification(keyword, roleName, status), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public AdminAccountResponseDto createStaff(CreateStaffRequestDto request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AdminAccountException(AdminAccountErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AdminAccountException(AdminAccountErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Account account = Account.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(roleService.findByCode(STAFF_ROLE))
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Admin account management created STAFF accountId={}", saved.getAccountId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminAccountResponseDto banAccount(UUID accountId) {
        Account account = findAccount(accountId);
        if (hasRole(account, ADMIN_ROLE)) {
            throw new AdminAccountException(AdminAccountErrorCode.CANNOT_BAN_ADMIN);
        }

        account.setStatus(AccountStatus.BANNED);
        Account saved = accountRepository.save(account);
        log.info("Admin account management banned accountId={}", accountId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AdminAccountResponseDto unbanAccount(UUID accountId) {
        Account account = findAccount(accountId);
        account.setStatus(AccountStatus.ACTIVE);
        Account saved = accountRepository.save(account);
        log.info("Admin account management unbanned accountId={}", accountId);
        return toResponse(saved);
    }

    private Account findAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AdminAccountException(AdminAccountErrorCode.ACCOUNT_NOT_FOUND));
    }

    private boolean hasRole(Account account, String roleCode) {
        return account.getRole() != null && roleCode.equalsIgnoreCase(account.getRole().getCode());
    }

    private AdminAccountResponseDto toResponse(Account account) {
        return AdminAccountResponseDto.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .username(account.getUsername())
                .fullName(account.getFullName())
                .avatarUrl(account.getAvatarUrl())
                .roleName(account.getRole() == null ? null : account.getRole().getCode())
                .status(account.getStatus() == null ? null : account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
