package com.talex.server.controllers.auth;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.auth.CreateStaffRequestDto;
import com.talex.server.dtos.responses.auth.AdminAccountResponseDto;
import com.talex.server.services.auth.AdminAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
@Tag(name = "Admin - Accounts", description = "API quản lý tài khoản dành cho Admin/Staff")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lấy danh sách tài khoản có phân trang và lọc")
    public ResponseEntity<BaseResponse> getAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminAccountResponseDto> accounts = adminAccountService.getAccounts(keyword, roleName, status, pageable);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách tài khoản thành công")
                .data(accounts)
                .build());
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo tài khoản Staff")
    public ResponseEntity<BaseResponse> createStaff(@Valid @RequestBody CreateStaffRequestDto request) {
        AdminAccountResponseDto created = adminAccountService.createStaff(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.builder()
                .code(201)
                .message("Tạo tài khoản Staff thành công")
                .data(created)
                .build());
    }

    @PatchMapping("/{accountId}/ban")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Khóa tài khoản")
    public ResponseEntity<BaseResponse> banAccount(@PathVariable UUID accountId) {
        AdminAccountResponseDto banned = adminAccountService.banAccount(accountId);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Khóa tài khoản thành công")
                .data(banned)
                .build());
    }

    @PatchMapping("/{accountId}/unban")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Mở khóa tài khoản")
    public ResponseEntity<BaseResponse> unbanAccount(@PathVariable UUID accountId) {
        AdminAccountResponseDto unbanned = adminAccountService.unbanAccount(accountId);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Mở khóa tài khoản thành công")
                .data(unbanned)
                .build());
    }
}
