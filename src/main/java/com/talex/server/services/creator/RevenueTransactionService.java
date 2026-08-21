package com.talex.server.services.creator;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.revenue.response.RevenueSummaryResponseDto;
import com.talex.server.dtos.revenue.response.RevenueTimeSeriesResponseDto;
import com.talex.server.dtos.revenue.response.RevenueTransactionDto;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.entities.series.EpisodeUnlockedContent;
import com.talex.server.entities.transaction.Order;

import java.time.LocalDateTime;
import java.util.List;

public interface RevenueTransactionService {
    /**
     * Tạo giao dịch doanh thu cho Creator khi có đơn hàng mua nội dung (EPISODE / COMBO) thành công.
     *
     * @param order Đơn hàng
     * @param unlockedContents Danh sách các EpisodeUnlockedContent được tạo từ order
     * @return RevenueTransaction đã lưu
     */
    RevenueTransaction createFromEpisodeOrder(Order order, List<EpisodeUnlockedContent> unlockedContents);

    // API 1: Danh sách tất cả biến động doanh thu có phân trang (Sort created_at DESC)
    BasePageResponse<RevenueTransactionDto> getAllTransactions(String creatorId, int page, int pageSize);

    // API 2: Thống kê tổng quan theo type & 3 field tổng kết trong khoảng thời gian
    RevenueSummaryResponseDto getRevenueSummary(String creatorId, LocalDateTime startDate, LocalDateTime endDate);

    // API 3: Thống kê danh sách chuỗi thời gian (dynamic: HOUR, DAY, MONTH, YEAR)
    List<RevenueTimeSeriesResponseDto> getRevenueTimeSeries(String creatorId, LocalDateTime startDate, LocalDateTime endDate);
}