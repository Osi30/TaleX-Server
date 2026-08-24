package com.talex.server.controllers.subscription;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.subscription.response.SubscriptionResultResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionRevenueLogDetailResponseDto;
import com.talex.server.services.subscription.SubscriptionResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription-results")
@RequiredArgsConstructor
@Tag(name = "Subscription Results", description = "API kết quả chia sẻ doanh thu gói đăng ký hàng tháng")
public class SubscriptionResultController {

    private final SubscriptionResultService subscriptionResultService;

    @GetMapping("/by-month-year")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy thông tin SubscriptionResult theo Tháng và Năm",
            description = "Truyền vào year và month (vd: 2026, 7 -> 2026-07) để lấy ra thông tin tổng quan SubscriptionResult bao gồm ID."
    )
    public ResponseEntity<BaseResponse> getSubscriptionResultByMonthYear(
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<SubscriptionResultResponseDto> data = subscriptionResultService.getSubscriptionResultByMonthYear(year, month);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(data)
                .build());
    }

    @GetMapping("/{id}/revenue-logs")
//    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "Lấy danh sách SubscriptionRevenueLog chi tiết theo SubscriptionResult ID",
            description = "Truyền vào ID của SubscriptionResult để lấy ra danh sách log phân bổ doanh thu chi tiết kèm username, avatarUrl của creator, episode title, number và series title."
    )
    public ResponseEntity<BaseResponse> getRevenueLogsByResultId(
            @PathVariable("id") String subscriptionResultId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<SubscriptionRevenueLogDetailResponseDto> pageResponse = subscriptionResultService
                .getRevenueLogsByResultId(subscriptionResultId, page, pageSize);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }
}