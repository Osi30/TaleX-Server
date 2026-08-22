package com.talex.server.controllers.order;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.BaseResponse;
import com.talex.server.dtos.revenue.response.RevenueSummaryResponseDto;
import com.talex.server.dtos.revenue.response.RevenueTimeSeriesResponseDto;
import com.talex.server.dtos.revenue.response.RevenueTransactionDto;
import com.talex.server.dtos.settlement.episode.TotalEpisodeRevenueDto;
import com.talex.server.dtos.settlement.series.TotalSeriesRevenueDto;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.creator.RevenueTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revenue-transactions")
@RequiredArgsConstructor
@Tag(name = "Revenue Transactions", description = "API quản lý và thống kê biến động doanh thu của Creator")
public class RevenueTransactionController {

    private final RevenueTransactionService revenueTransactionService;
    private final CreatorService creatorService;

    @GetMapping
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Lấy danh sách biến động doanh thu (phân trang, sắp xếp từ mới nhất)",
            description = "Trả về danh sách lịch sử biến động doanh thu. Có thể truyền creatorId để lọc theo nhà sáng tạo."
    )
    public ResponseEntity<BaseResponse> getAllTransactions(
            @CurrentAccountId UUID accountId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        BasePageResponse<RevenueTransactionDto> pageResponse =
                revenueTransactionService.getAllTransactions(
                        creatorService.getIdByAccountId(accountId), page, pageSize
                );
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(pageResponse)
                .build());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Lấy tổng quan thống kê doanh thu theo khoảng thời gian",
            description = "Gom nhóm theo RevenueTransactionType và tính tổng số tiền. Trả về 3 số tổng: Tổng doanh thu (PREMIUM + CONTENT), Tổng phạt (PENALTY_DEDUCTION), và Tổng điều chỉnh (ADJUSTMENT)."
    )
    public ResponseEntity<BaseResponse> getRevenueSummary(
            @CurrentAccountId UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        RevenueSummaryResponseDto summary = revenueTransactionService
                .getRevenueSummary(creatorService.getIdByAccountId(accountId), startDate, endDate);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(summary)
                .build());
    }

    @GetMapping("/time-series")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "Lấy thống kê doanh thu theo chuỗi thời gian (Biểu đồ)",
            description = "Tự động gom nhóm dựa trên khoảng thời gian truyền vào: < 7 ngày gom theo GIỜ, < 30 ngày gom theo NGÀY, < 12 tháng gom theo THÁNG, >= 12 tháng gom theo NĂM."
    )
    public ResponseEntity<BaseResponse> getRevenueTimeSeries(
            @CurrentAccountId UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<RevenueTimeSeriesResponseDto> timeSeries =
                revenueTransactionService
                        .getRevenueTimeSeries(creatorService.getIdByAccountId(accountId), startDate, endDate);

        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("OK")
                .data(timeSeries)
                .build());
    }

    @GetMapping("/episodes/{episodeId}/unsettled-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREATOR')")
    @Operation(
            summary = "Lấy tổng doanh thu chưa quyết toán của Episode (Bao gồm mua lẻ & Premium)",
            description = "Tính tổng doanh thu chưa quyết toán của Episode từ cả đơn mua lẻ (ORDER) và gói chia sẻ Subscription (PREMIUM_RESULT)."
    )
    public ResponseEntity<BaseResponse> getEpisodesUnsettledRevenue(@PathVariable String episodeId) {
        TotalEpisodeRevenueDto data = revenueTransactionService.getTotalUnsettledRevenueByEpisodeId(episodeId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy doanh thu chưa quyết toán của tập thành công")
                .data(data)
                .build());
    }

    @GetMapping("/series/{seriesId}/unsettled-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREATOR')")
    @Operation(
            summary = "Lấy tổng doanh thu chưa quyết toán của Series (Bao gồm mua lẻ & Premium)",
            description = "Tính tổng doanh thu chưa quyết toán của Series từ cả đơn mua lẻ (ORDER) và gói chia sẻ Subscription (PREMIUM_RESULT)."
    )
    public ResponseEntity<BaseResponse> getSeriesUnsettledRevenue(@PathVariable String seriesId) {
        TotalSeriesRevenueDto data = revenueTransactionService.getTotalUnsettledRevenueBySeriesId(seriesId);
        return ResponseEntity.ok(BaseResponse.builder()
                .code(200)
                .message("Lấy doanh thu chưa quyết toán của tập thành công")
                .data(data)
                .build());
    }
}