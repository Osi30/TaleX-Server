package com.talex.server.controllers.subscription;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.subscription.response.SubscriptionStatDetailResponseDto;
import com.talex.server.services.subscription.SubscriptionStatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription-stats")
@RequiredArgsConstructor
@Tag(name = "Subscription Stats", description = "API quản lý và truy vấn thống kê lượt xem theo gói đăng ký")
public class SubscriptionStatController {

    private final SubscriptionStatService subscriptionStatService;

    @GetMapping("/account-subscription/{accountSubscriptionId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "Lấy danh sách thống kê chi tiết theo Account Subscription ID",
            description = "Trả về danh sách lượt xem phân trang gồm tiêu đề episode, số tập, tên series, username và avatarUrl của Creator."
    )
    public ResponseEntity<BaseResponse> getDetailedStatsByAccountSubscriptionId(
            @PathVariable("accountSubscriptionId") String accountSubscriptionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<SubscriptionStatDetailResponseDto> pageResponse = subscriptionStatService
                .getDetailedStatsByAccountSubscriptionId(accountSubscriptionId, page, pageSize);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }
}