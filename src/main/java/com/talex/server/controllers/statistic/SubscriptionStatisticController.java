package com.talex.server.controllers.statistic;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.statistics.subscription.SubscriptionRevenueDetailDto;
import com.talex.server.dtos.statistics.subscription.SubscriptionRevenueOverviewDto;
import com.talex.server.services.statistic.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics/subscription")
@RequiredArgsConstructor
@Tag(name = "Subscription Statistics", description = "API thống kê doanh thu gói Premium (SUBSCRIPTION) dành cho Admin")
public class SubscriptionStatisticController {

    private final StatisticService subscriptionStatisticService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thống kê tổng quan doanh thu gói Premium",
            description = "Trả về tổng doanh thu (totalAmount), tổng thuế VAT (vatAmount) và doanh thu thuần (total - vat) của các đơn hàng COMPLETED có itemType là SUBSCRIPTION."
    )
    public ResponseEntity<BaseResponse> getSubscriptionOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        SubscriptionRevenueOverviewDto overview = subscriptionStatisticService.getSubscriptionOverview(startTime, endTime);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thống kê tổng quan Premium thành công")
                .data(overview)
                .build());
    }

    @GetMapping("/details")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thống kê chi tiết doanh thu gói Premium theo chuỗi thời gian (Dùng cho Biểu đồ)",
            description = "Gom nhóm động theo khoảng thời gian: <7 ngày (Giờ), <30 ngày (Ngày), <12 tháng (Tháng), >=12 tháng (Năm). Trả về danh sách gồm doanh thu, VAT và doanh thu ròng."
    )
    public ResponseEntity<BaseResponse> getSubscriptionDetails(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        List<SubscriptionRevenueDetailDto> details = subscriptionStatisticService.getSubscriptionDetails(startTime, endTime);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách chi tiết biểu đồ Premium thành công")
                .data(details)
                .build());
    }
}