package com.talex.server.controllers.subscription;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.requests.subscription.AccountSubscriptionRequestDto;
import com.talex.server.dtos.responses.subscription.AccountSubscriptionResponseDto;
import com.talex.server.services.subscription.IAccountSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account-subscriptions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Account Subscriptions", description = "API quản lý đăng ký và hiển thị gói dịch vụ của tài khoản")
public class AccountSubscriptionController {
        private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_STAFF", "ROLE_ADMIN");

        private final IAccountSubscriptionService accountSubscriptionService;

        @PostMapping
        @Operation(summary = "Tạo đăng ký gói tài khoản", description = "Tạo đăng ký gói dịch vụ cho tài khoản đang đăng nhập.")
        public ResponseEntity<BaseResponse> create(
                        @RequestBody AccountSubscriptionRequestDto request,
                        @CurrentAccountId UUID accountId) {
                request.setAccountId(accountId);
                AccountSubscriptionResponseDto response = accountSubscriptionService.createAccountSubscription(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(BaseResponse.builder()
                                                .code(201)
                                                .message("Account subscription created")
                                                .data(response)
                                                .build());
        }

        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
        @Operation(summary = "Lấy danh sách gói đăng ký", description = "Lấy danh sách các gói đăng ký theo điều kiện lọc và phân trang. Chỉ Admin/Staff.")
        public ResponseEntity<BaseResponse> list(
                        @RequestParam(required = false) Map<String, Object> criteria,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false) String sortDirection,
                        @RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "20") Integer pageSize) {
                BaseFilterRequestDto filterRequest = BaseFilterRequestDto.builder()
                                .criteria(criteria)
                                .sortBy(sortBy)
                                .sortDirection(sortDirection)
                                .page(page)
                                .pageSize(pageSize)
                                .build();

                BasePageResponse<AccountSubscriptionResponseDto> pageResponse = accountSubscriptionService
                                .filterAndSortAccountSubscriptions(filterRequest);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(pageResponse)
                                .build());
        }

        @GetMapping("/own")
        @Operation(summary = "Lấy gói đăng ký của tài khoản", description = "Lấy danh sách đăng ký đang thuộc về tài khoản đang đăng nhập.")
        public ResponseEntity<BaseResponse> own(
                        @CurrentAccountId UUID accountId,
                        @RequestParam(required = false) Map<String, Object> criteria,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false) String sortDirection,
                        @RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "20") Integer pageSize) {
                criteria.put("accountId", accountId.toString());
                BaseFilterRequestDto filterRequest = BaseFilterRequestDto.builder()
                                .criteria(criteria)
                                .sortBy(sortBy)
                                .sortDirection(sortDirection)
                                .page(page)
                                .pageSize(pageSize)
                                .build();

                BasePageResponse<AccountSubscriptionResponseDto> pageResponse = accountSubscriptionService
                                .filterAndSortAccountSubscriptions(filterRequest);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(pageResponse)
                                .build());
        }

        @GetMapping("/{accountSubscriptionId}")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
        @Operation(summary = "Lấy gói đăng ký theo ID", description = "Trả về thông tin chi tiết của gói đăng ký theo ID. Chỉ Admin/Staff.")
        public ResponseEntity<BaseResponse> getById(@PathVariable String accountSubscriptionId) {
                AccountSubscriptionResponseDto response = accountSubscriptionService
                                .getAccountSubscriptionById(accountSubscriptionId);
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(200)
                                .message("OK")
                                .data(response)
                                .build());
        }

        @DeleteMapping("/{accountSubscriptionId}")
        @Operation(summary = "Hủy gói đăng ký", description = "Hủy gói đăng ký theo ID (chủ sở hữu hoặc Admin/Staff), trả về mã 204 khi thành công.")
        public ResponseEntity<BaseResponse> cancel(
                        @PathVariable String accountSubscriptionId,
                        @CurrentAccountId UUID accountId) {
                accountSubscriptionService.cancelAccountSubscription(accountSubscriptionId, accountId, isPrivileged());
                return ResponseEntity.ok(BaseResponse.builder()
                                .code(204)
                                .message("Account subscription cancelled")
                                .data(null)
                                .build());
        }

        private boolean isPrivileged() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null) {
                        return false;
                }
                return authentication.getAuthorities().stream()
                                .anyMatch(authority -> PRIVILEGED_ROLES.contains(authority.getAuthority()));
        }
}
