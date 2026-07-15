package com.talex.server.services.auth;

import com.talex.server.dtos.requests.auth.CreateStaffRequestDto;
import com.talex.server.dtos.responses.auth.AdminAccountResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminAccountService {

    Page<AdminAccountResponseDto> getAccounts(String keyword, String roleName, String status, Pageable pageable);

    AdminAccountResponseDto createStaff(CreateStaffRequestDto request);

    AdminAccountResponseDto banAccount(UUID accountId);

    AdminAccountResponseDto unbanAccount(UUID accountId);
}
