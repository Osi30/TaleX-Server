package com.talex.server.controllers.statistic;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.statistics.content.ContentRevenueDetailDto;
import com.talex.server.dtos.statistics.content.ContentRevenueOverviewDto;
import com.talex.server.services.statistic.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @GetMapping("/export-excel")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xuất báo cáo Excel Combo & Episode", description = "Xuất file Excel gồm tab Thống kê tổng quan và tab Chi tiết các đơn hàng Episode và Combo.")
    public ResponseEntity<byte[]> exportContentExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        byte[] excelBytes = contentStatisticService.exportContentExcel(startTime, endTime);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao_Cao_Combo_Episode.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/export-excel-by-item")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xuất Excel các đơn hàng đã hoàn tất theo itemId (Combo hoặc Episode)",
            description = "Lấy danh sách các đơn hàng có trạng thái COMPLETED tương ứng với itemId (EPISODE hoặc COMBO) truyền vào."
    )
    public ResponseEntity<byte[]> exportContentExcelByItemId(
            @RequestParam String itemId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        byte[] excelBytes = contentStatisticService.exportContentExcelByItemId(itemId, startTime, endTime);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao_Cao_Don_Hang_Item_" + itemId + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/export-excel-by-series")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xuất Excel các đơn hàng đã hoàn tất theo seriesId",
            description = "Lấy danh sách các đơn hàng đã hoàn tất (COMPLETED) của tất cả các Episode thuộc Series ID truyền vào."
    )
    public ResponseEntity<byte[]> exportContentExcelBySeriesId(
            @RequestParam String seriesId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        byte[] excelBytes = contentStatisticService.exportContentExcelBySeriesId(seriesId, startTime, endTime);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao_Cao_Don_Hang_Series_" + seriesId + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}