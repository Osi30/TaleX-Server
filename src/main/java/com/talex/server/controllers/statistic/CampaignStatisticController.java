package com.talex.server.controllers.statistic;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.statistics.campaign.CampaignRevenueDetailDto;
import com.talex.server.dtos.statistics.campaign.CampaignRevenueOverviewDto;
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
@RequestMapping("/api/v1/statistics/campaign")
@RequiredArgsConstructor
@Tag(name = "Campaign Statistics", description = "API thống kê doanh thu gói Campaign dành cho Admin")
public class CampaignStatisticController {

    private final StatisticService campaignStatisticService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thống kê tổng quan doanh thu Campaign",
            description = "Trả về tổng doanh thu (totalAmount), tổng thuế VAT (vatAmount) và doanh thu thuần (total - vat) của các đơn hàng COMPLETED."
    )
    public ResponseEntity<BaseResponse> getCampaignOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        CampaignRevenueOverviewDto overview = campaignStatisticService.getCampaignOverview(startTime, endTime);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thống kê tổng quan Campaign thành công")
                .data(overview)
                .build());
    }

    @GetMapping("/details")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thống kê chi tiết doanh thu Campaign theo chuỗi thời gian (Dùng cho Biểu đồ)",
            description = "Gom nhóm động theo khoảng thời gian: <7 ngày (Giờ), <30 ngày (Ngày), <12 tháng (Tháng), >=12 tháng (Năm). Trả về danh sách gồm doanh thu, VAT và doanh thu ròng."
    )
    public ResponseEntity<BaseResponse> getCampaignDetails(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        List<CampaignRevenueDetailDto> details = campaignStatisticService.getCampaignDetails(startTime, endTime);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách chi tiết biểu đồ Campaign thành công")
                .data(details)
                .build());
    }
}