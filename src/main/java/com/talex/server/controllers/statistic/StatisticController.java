package com.talex.server.controllers.statistic;

import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.statistics.StatisticResponseDto;
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

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "API thống kê doanh thu, GMV, VAT và Coin của hệ thống")
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lấy dữ liệu thống kê tài chính",
            description = "Trả về tổng GMV, doanh thu thuần (đã trừ thuế, chưa tính phần chia sẻ cho nhà sáng tạo và tiền hoàn trả), " +
                    "tổng VAT và tổng Coin sử dụng trong khoảng thời gian. " +
                    "Nếu bỏ trống startTime và endTime, hệ thống tự động lấy 6 tháng gần nhất. " +
                    "Tự động chia theo Ngày (nếu chọn dưới 2 tháng) hoặc Tháng (nếu chọn từ 2 tháng trở lên). " +
                    "Giới hạn khoảng thời gian tối đa là 1 năm."
    )
    public ResponseEntity<BaseResponse> getOrderStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        StatisticResponseDto data = statisticService.getOrderStatistics(startTime, endTime);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy thông tin thống kê thành công")
                .data(data)
                .build());
    }

    @GetMapping("/export-excel")
//    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Xuất file Excel tổng quan toàn bộ hệ thống (4 tabs)",
            description = "Tạo file Excel gồm 4 tabs: 1 Tab tổng quan hệ thống và 3 Tabs chi tiết cho Campaign, Combo & Episode, Premium."
    )
    public ResponseEntity<byte[]> exportAllExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        byte[] excelBytes = statisticService.exportAllExcel(startTime, endTime);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bao_Cao_Tong_Quan_He_Thong.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}