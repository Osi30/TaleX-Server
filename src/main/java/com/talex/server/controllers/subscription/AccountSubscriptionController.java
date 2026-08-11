package com.talex.server.controllers.subscription;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.subscription.request.AccountSubscriptionRequestDto;
import com.talex.server.dtos.subscription.response.AccountSubscriptionResponseDto;
import com.talex.server.dtos.subscription.response.CreatorPoolDetailResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionStatResponseDto;
import com.talex.server.services.subscription.AccountSubscriptionService;
import com.talex.server.services.subscription.SubscriptionStatService;
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
@Tag(name = "Account Subscriptions", description = "API quản lý đăng ký và hiển thị gói dịch vụ của tài khoản")
public class AccountSubscriptionController {
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_STAFF", "ROLE_ADMIN");

    private final AccountSubscriptionService accountSubscriptionService;
    private final SubscriptionStatService subscriptionStatService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Operation(summary = "Cấp đăng ký gói tài khoản thủ công", description = "Admin/Staff cấp thủ công gói dịch vụ cho một tài khoản đích (accountId truyền trong body). Mua Premium thông thường phải đi qua luồng Order/thanh toán, không dùng endpoint này.")
    public ResponseEntity<BaseResponse> create(
            @jakarta.validation.Valid @RequestBody AccountSubscriptionRequestDto request) {
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy gói đăng ký đang hoạt động của tài khoản",
            description = "Lấy thông tin gói đăng ký active của người dùng dựa vào tham số thời gian now (mặc định thời gian hiện tại nếu không truyền)."
    )
    public ResponseEntity<BaseResponse> getActiveSubscription(
            @CurrentAccountId UUID accountId
    ) {
        AccountSubscriptionResponseDto response = accountSubscriptionService
                .getActiveAccountSubscription(accountId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(response)
                .build());
    }

    @GetMapping("/pool-details")
    @Operation(
            summary = "Lấy chi tiết các Account Subscription trong pool của tháng",
            description = "Trả về danh sách phân trang chi tiết thông tin doanh thu, lượt xem và account trong pool Creator của tháng."
    )
    public ResponseEntity<BaseResponse> getCreatorPoolDetails(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam int month,
            @RequestParam(required = false) String subscriptionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<CreatorPoolDetailResponseDto> pageResponse = accountSubscriptionService
                .getCreatorPoolDetails(year, month, subscriptionId, page, pageSize);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/{accountSubscriptionId}/stats")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy danh sách thống kê SubscriptionStat theo AccountSubscription ID",
            description = "Trả về danh sách lượt xem chi tiết từng tập phim, bao gồm email nhà sáng tạo, thông tin tập phim và bộ phim."
    )
    public ResponseEntity<BaseResponse> getStatsByAccountSubscriptionId(
            @PathVariable("accountSubscriptionId") String accountSubscriptionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<SubscriptionStatResponseDto> pageResponse = subscriptionStatService
                .getStatsByAccountSubscriptionId(accountSubscriptionId, page, pageSize);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
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
