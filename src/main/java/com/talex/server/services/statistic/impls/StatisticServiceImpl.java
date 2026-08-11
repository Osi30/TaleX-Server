package com.talex.server.services.statistic.impls;

import com.talex.server.dtos.statistics.StatisticOverviewDto;
import com.talex.server.dtos.statistics.StatisticResponseDto;
import com.talex.server.dtos.statistics.StatisticTrendDto;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.records.OrderStatisticData;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.statistic.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final OrderRepository orderRepository;

    @Override
    public StatisticResponseDto getOrderStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Xử lý giá trị mặc định nếu bỏ trống (6 tháng gần nhất)
        if (endTime == null) {
            endTime = now;
        }
        if (startTime == null) {
            startTime = endTime.minusMonths(6);
        }

        // Validate thời gian
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Thời gian bắt đầu (startTime) phải nhỏ hơn thời gian kết thúc (endTime).");
        }

        // 2. Giới hạn không được vượt quá 1 năm (365/366 ngày)
        if (startTime.plusYears(1).isBefore(endTime)) {
            throw new IllegalArgumentException("Khung thời gian truy vấn tối đa chỉ được 1 năm.");
        }

        // 3. Tự động xác định kiểu gom nhóm theo ngày hay tháng (nhỏ hơn 2 tháng -> chia theo ngày)
        boolean isLessThanTwoMonths = startTime.plusMonths(2).isAfter(endTime);
        String dateFormatPattern = isLessThanTwoMonths ? "YYYY-MM-DD" : "YYYY-MM";

        // Chỉ thống kê các đơn hàng đã hoàn tất (COMPLETED)
        String completedStatus = OrderStatus.COMPLETED.name();

        // 4. Truy vấn dữ liệu Tổng quan (Overview)
        OrderStatisticData overviewProjection = orderRepository.getOverviewStatistic(
                completedStatus, startTime, endTime
        );

        StatisticOverviewDto overview = StatisticOverviewDto.builder()
                .gmv(overviewProjection != null ? overviewProjection.gmv() : java.math.BigDecimal.ZERO)
                .totalNetRevenue(overviewProjection != null ? overviewProjection.netRevenue() : java.math.BigDecimal.ZERO)
                .totalVat(overviewProjection != null ? overviewProjection.vatAmount() : java.math.BigDecimal.ZERO)
                .totalCoin(overviewProjection != null ? overviewProjection.totalCoin().longValue() : 0L)
                .build();

        // 5. Truy vấn dữ liệu theo thời gian (Trends)
        List<OrderStatisticData> trendProjections = orderRepository.getGroupedStatistics(
                completedStatus, startTime, endTime, dateFormatPattern
        );

        List<StatisticTrendDto> trends = trendProjections.stream()
                .map(proj -> StatisticTrendDto.builder()
                        .period(proj.period())
                        .gmv(proj.gmv())
                        .netRevenue(proj.netRevenue())
                        .vatAmount(proj.vatAmount())
                        .totalCoin(proj.totalCoin() != null ? proj.totalCoin().longValue() : 0L)
                        .build())
                .toList();

        return StatisticResponseDto.builder()
                .overview(overview)
                .trends(trends)
                .build();
    }
}