package com.talex.server.controllers.statistic;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.statistics.content.ContentRevenueDetailDto;
import com.talex.server.dtos.statistics.content.ContentRevenueOverviewDto;
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
@RequestMapping("/api/v1/statistics/content")
@RequiredArgsConstructor
@Tag(name = "Content Sales Statistics", description = "API thống kê doanh thu bán Tập (EPISODE) và Combo dành cho Admin")
public class ContentStatisticController {

    private final StatisticService contentStatisticService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thống kê tổng quan doanh thu bán Tập & Combo",
            description = "Trả về tổng doanh thu, thuế VAT, số coin sử dụng, số tiền chia sẻ cho Creator và doanh thu thuần (Gross - VAT - Coin - Share) của các đơn hàng COMPLETED có itemType là EPISODE hoặc COMBO."
    )
    public ResponseEntity<BaseResponse> getContentOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        ContentRevenueOverviewDto overview = contentStatisticService.getContentOverview(startTime, endTime);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thống kê tổng quan doanh thu nội dung thành công")
                .data(overview)
                .build());
    }

    @GetMapping("/details")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thống kê chi tiết doanh thu bán Tập & Combo theo chuỗi thời gian (Biểu đồ)",
            description = "Gom nhóm động theo thời gian: <7 ngày (Giờ), <30 ngày (Ngày), <12 tháng (Tháng), >=12 tháng (Năm). Trả về chi tiết các chỉ số doanh thu, VAT, Coin, Share và doanh thu ròng."
    )
    public ResponseEntity<BaseResponse> getContentDetails(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        List<ContentRevenueDetailDto> details = contentStatisticService.getContentDetails(startTime, endTime);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy danh sách chi tiết biểu đồ doanh thu nội dung thành công")
                .data(details)
                .build());
    }
}