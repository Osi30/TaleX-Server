package com.talex.server.services;

import com.talex.server.dtos.requests.admin.CreateStaffRequestDto;
import com.talex.server.dtos.responses.admin.AdminAccountResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminAccountService {

    Page<AdminAccountResponseDto> getAccounts(String keyword, String roleName, String status, Pageable pageable);

    AdminAccountResponseDto createStaff(CreateStaffRequestDto request);

    AdminAccountResponseDto banAccount(UUID accountId);

    AdminAccountResponseDto unbanAccount(UUID accountId);
}
