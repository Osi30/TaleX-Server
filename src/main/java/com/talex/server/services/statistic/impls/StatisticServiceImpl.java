package com.talex.server.services.statistic.impls;

import com.talex.server.dtos.statistics.StatisticOverviewDto;
import com.talex.server.dtos.statistics.StatisticResponseDto;
import com.talex.server.dtos.statistics.StatisticTrendDto;
import com.talex.server.dtos.statistics.campaign.CampaignRevenueDetailDto;
import com.talex.server.dtos.statistics.campaign.CampaignRevenueOverviewDto;
import com.talex.server.dtos.statistics.campaign.CampaignStatisticData;
import com.talex.server.dtos.statistics.content.ContentRevenueDetailDto;
import com.talex.server.dtos.statistics.content.ContentRevenueOverviewDto;
import com.talex.server.dtos.statistics.content.ContentRevenueStatisticData;
import com.talex.server.dtos.statistics.subscription.SubscriptionRevenueDetailDto;
import com.talex.server.dtos.statistics.subscription.SubscriptionRevenueOverviewDto;
import com.talex.server.dtos.statistics.subscription.SubscriptionStatisticData;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.records.OrderStatisticData;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.statistic.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final OrderRepository orderRepository;
    private static final String CAMPAIGN_ITEM_TYPE = "ENGAGEMENT";
    private static final String SUBSCRIPTION_ITEM_TYPE = "SUBSCRIPTION";

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

    @Override
    @Transactional(readOnly = true)
    public CampaignRevenueOverviewDto getCampaignOverview(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        String completedStatus = OrderStatus.COMPLETED.name();
        CampaignStatisticData data = orderRepository.getCampaignOverviewStatistic(
                completedStatus, CAMPAIGN_ITEM_TYPE, startTime, endTime
        );

        if (data == null) {
            return CampaignRevenueOverviewDto.builder()
                    .totalGrossRevenue(BigDecimal.ZERO)
                    .totalVatAmount(BigDecimal.ZERO)
                    .totalNetRevenue(BigDecimal.ZERO)
                    .build();
        }

        return CampaignRevenueOverviewDto.builder()
                .totalGrossRevenue(data.grossRevenue())
                .totalVatAmount(data.vatAmount())
                .totalNetRevenue(data.netRevenue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignRevenueDetailDto> getCampaignDetails(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        // Tính khoảng cách giữa startTime và endTime để chọn định dạng gom nhóm
        long daysBetween = Duration.between(startTime, endTime).toDays();

        String dateFormatPattern;
        String groupUnit;

        if (daysBetween < 7) {
            dateFormatPattern = "YYYY-MM-DD HH24:00";
            groupUnit = "HOUR";
        } else if (daysBetween < 30) {
            dateFormatPattern = "YYYY-MM-DD";
            groupUnit = "DAY";
        } else if (startTime.plusMonths(12).isAfter(endTime)) { // Dưới 12 tháng
            dateFormatPattern = "YYYY-MM";
            groupUnit = "MONTH";
        } else { // Trên 12 tháng
            dateFormatPattern = "YYYY";
            groupUnit = "YEAR";
        }

        String completedStatus = OrderStatus.COMPLETED.name();
        List<CampaignStatisticData> rawDataList = orderRepository.getCampaignGroupedStatistics(
                completedStatus, CAMPAIGN_ITEM_TYPE, startTime, endTime, dateFormatPattern
        );

        return rawDataList.stream()
                .map(data -> CampaignRevenueDetailDto.builder()
                        .period(data.period())
                        .grossRevenue(data.grossRevenue())
                        .vatAmount(data.vatAmount())
                        .netRevenue(data.netRevenue())
                        .groupUnit(groupUnit)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContentRevenueOverviewDto getContentOverview(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        String completedStatus = OrderStatus.COMPLETED.name();
        ContentRevenueStatisticData data = orderRepository.getContentOverviewStatistic(
                completedStatus, startTime, endTime
        );

        if (data == null) {
            return ContentRevenueOverviewDto.builder()
                    .totalGrossRevenue(BigDecimal.ZERO)
                    .totalVatAmount(BigDecimal.ZERO)
                    .totalCoinAmount(BigDecimal.ZERO)
                    .totalCreatorShareAmount(BigDecimal.ZERO)
                    .totalNetRevenue(BigDecimal.ZERO)
                    .build();
        }

        return ContentRevenueOverviewDto.builder()
                .totalGrossRevenue(data.grossRevenue())
                .totalVatAmount(data.vatAmount())
                .totalCoinAmount(data.coinAmount())
                .totalCreatorShareAmount(data.creatorShareAmount())
                .totalNetRevenue(data.netRevenue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentRevenueDetailDto> getContentDetails(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        long daysBetween = Duration.between(startTime, endTime).toDays();

        String dateFormatPattern;
        String groupUnit;

        if (daysBetween < 7) {
            dateFormatPattern = "YYYY-MM-DD HH24:00";
            groupUnit = "HOUR";
        } else if (daysBetween < 30) {
            dateFormatPattern = "YYYY-MM-DD";
            groupUnit = "DAY";
        } else if (startTime.plusMonths(12).isAfter(endTime)) { // Dưới 12 tháng
            dateFormatPattern = "YYYY-MM";
            groupUnit = "MONTH";
        } else { // Từ 12 tháng trở lên
            dateFormatPattern = "YYYY";
            groupUnit = "YEAR";
        }

        String completedStatus = OrderStatus.COMPLETED.name();
        List<ContentRevenueStatisticData> rawDataList = orderRepository.getContentGroupedStatistics(
                completedStatus, startTime, endTime, dateFormatPattern
        );

        return rawDataList.stream()
                .map(data -> ContentRevenueDetailDto.builder()
                        .period(data.period())
                        .grossRevenue(data.grossRevenue())
                        .vatAmount(data.vatAmount())
                        .coinAmount(data.coinAmount())
                        .creatorShareAmount(data.creatorShareAmount())
                        .netRevenue(data.netRevenue())
                        .groupUnit(groupUnit)
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionRevenueOverviewDto getSubscriptionOverview(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        String completedStatus = OrderStatus.COMPLETED.name();
        SubscriptionStatisticData data = orderRepository.getSubscriptionOverviewStatistic(
                completedStatus, SUBSCRIPTION_ITEM_TYPE, startTime, endTime
        );

        if (data == null) {
            return SubscriptionRevenueOverviewDto.builder()
                    .totalGrossRevenue(BigDecimal.ZERO)
                    .totalVatAmount(BigDecimal.ZERO)
                    .totalNetRevenue(BigDecimal.ZERO)
                    .build();
        }

        return SubscriptionRevenueOverviewDto.builder()
                .totalGrossRevenue(data.grossRevenue())
                .totalVatAmount(data.vatAmount())
                .totalNetRevenue(data.netRevenue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionRevenueDetailDto> getSubscriptionDetails(LocalDateTime startTime, LocalDateTime endTime) {
        validateTimeRange(startTime, endTime);

        long daysBetween = Duration.between(startTime, endTime).toDays();

        String dateFormatPattern;
        String groupUnit;

        if (daysBetween < 7) {
            dateFormatPattern = "YYYY-MM-DD HH24:00";
            groupUnit = "HOUR";
        } else if (daysBetween < 30) {
            dateFormatPattern = "YYYY-MM-DD";
            groupUnit = "DAY";
        } else if (startTime.plusMonths(12).isAfter(endTime)) { // Dưới 12 tháng
            dateFormatPattern = "YYYY-MM";
            groupUnit = "MONTH";
        } else { // Từ 12 tháng trở lên
            dateFormatPattern = "YYYY";
            groupUnit = "YEAR";
        }

        String completedStatus = OrderStatus.COMPLETED.name();
        List<SubscriptionStatisticData> rawDataList = orderRepository.getSubscriptionGroupedStatistics(
                completedStatus, SUBSCRIPTION_ITEM_TYPE, startTime, endTime, dateFormatPattern
        );

        return rawDataList.stream()
                .map(data -> SubscriptionRevenueDetailDto.builder()
                        .period(data.period())
                        .grossRevenue(data.grossRevenue())
                        .vatAmount(data.vatAmount())
                        .netRevenue(data.netRevenue())
                        .groupUnit(groupUnit)
                        .build())
                .toList();
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime và endTime không được để trống!");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("startTime phải nhỏ hơn hoặc bằng endTime!");
        }
    }
}