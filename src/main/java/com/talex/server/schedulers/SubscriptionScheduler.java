package com.talex.server.schedulers;

import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.repositories.subscription.SubscriptionRepository;
import com.talex.server.services.subscription.SubscriptionRevenueService;
import com.talex.server.services.subscription.SubscriptionStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {
    private final SubscriptionStatService subscriptionStatService;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionRevenueService subscriptionRevenueService;
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Cron job tự động chạy mỗi ngày.
     * Tính toán và lưu lượt xem hợp lệ vào sub stat.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processSubscriptionStats() {
        log.info("Triggering scheduled processSubscriptionStats job...");
        int processed = subscriptionStatService.processSubscriptionStats();
        log.info("Scheduled job completed. Total sessions processed: {}", processed);
    }

    /**
     * Cron job tự động chạy vào 00:00:00 ngày 01 đầu mỗi tháng.
     * Tính toán và lưu doanh thu Rule X cho tháng vừa qua.
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void processMonthlyRevenueDistribution() {
        // Lấy thời gian tháng trước (LocalDate hiện tại trừ đi 1 tháng)
        String previousMonthYear = LocalDate.now().minusMonths(1).format(MONTH_YEAR_FORMATTER);
        log.info("Starting scheduled monthly Rule X revenue distribution for month: {}", previousMonthYear);

        // 1. Query tất cả Subscriptions (bao gồm cả isDeleted = true và false)
        List<Subscription> subscriptions = subscriptionRepository.findAll();

        if (subscriptions.isEmpty()) {
            log.info("No subscriptions found in the database.");
            return;
        }

        int processedCount = 0;
        int skippedCount = 0;

        // 2. Duyệt qua từng Subscription để kiểm tra và xử lý
        for (Subscription subscription : subscriptions) {
            try {
                // Lấy request dto từ stats của tháng trước
                RuleXCalculationRequestDto requestDto = subscriptionStatService.getRuleXRequestFromStats(
                        previousMonthYear,
                        subscription
                );

                // Nếu danh sách users không rỗng thì tiến hành tính toán và lưu
                if (requestDto != null && requestDto.getUsers() != null && !requestDto.getUsers().isEmpty()) {
                    subscriptionStatService.calculateAndSaveRevenue(previousMonthYear, subscription, false);
                    processedCount++;
                    log.info("Successfully processed revenue for subscriptionId: {}, monthYear: {}",
                            subscription.getSubscriptionId(), previousMonthYear);
                } else {
                    skippedCount++;
                    log.info("Skipped calculation for subscriptionId: {} (No user streams found for {})",
                            subscription.getSubscriptionId(), previousMonthYear);
                }
            } catch (Exception e) {
                log.error("Error calculating monthly revenue for subscriptionId: {}",
                        subscription.getSubscriptionId(), e);
            }
        }

        log.info("Completed monthly Rule X revenue distribution for {}. Processed: {}, Skipped: {}",
                previousMonthYear, processedCount, skippedCount);
    }

    /**
     * Cron job tự động chạy vào 01:00:00 ngày 01 đầu mỗi tháng (trễ hơn 1 tiếng).
     * Phân bổ và cộng tiền trực tiếp vào ví cho các Creator từ kết quả Premium Revenue tháng trước.
     */
    @Scheduled(cron = "0 0 1 1 * *")
    public void processMonthlyCreatorRevenuePayout() {
        String previousMonthYear = LocalDate.now().minusMonths(1).format(MONTH_YEAR_FORMATTER);
        log.info("Starting scheduled monthly Creator Premium Revenue payout for month: {}", previousMonthYear);

        try {
            subscriptionRevenueService.processAndDistributePremiumRevenue(previousMonthYear, false);
            log.info("Successfully completed Creator Premium Revenue payout for month: {}", previousMonthYear);
        } catch (Exception e) {
            log.error("Error processing Creator Premium Revenue payout for month: {}", previousMonthYear, e);
        }
    }
}