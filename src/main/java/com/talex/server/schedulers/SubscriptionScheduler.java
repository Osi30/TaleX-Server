package com.talex.server.schedulers;

import com.talex.server.entities.subscription.SubscriptionResult;
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
        // Lấy thời gian tháng trước (YYYY-MM)
        String previousMonthYear = LocalDate.now()
                .minusMonths(1).format(MONTH_YEAR_FORMATTER);

        try {
            // Tự động quét stats + order, tính toán Rule X cho từng nhóm gói và lưu DB
            List<SubscriptionResult> results = subscriptionStatService.calculateAndSaveRevenue(previousMonthYear, false);

            if (!results.isEmpty()) {
                log.info("Successfully completed monthly Rule X revenue distribution for {}. Total order groups processed: {}",
                        previousMonthYear, results.size());
            } else {
                log.info("Skipped monthly revenue distribution for {} (No user streams or stats found)", previousMonthYear);
            }
        } catch (Exception e) {
            log.error("Error processing monthly Rule X revenue distribution for monthYear: {}", previousMonthYear, e);
        }
    }

    /**
     * Cron job tự động chạy vào 01:00:00 ngày 01 đầu mỗi tháng (trễ hơn 1 tiếng).
     * Phân bổ và cộng tiền trực tiếp vào ví cho các Creator từ kết quả Premium Revenue tháng trước.
     */
    @Scheduled(cron = "0 0 1 1 * *")
    public void processMonthlyCreatorRevenuePayout() {
        LocalDate previousMonthYear = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        try {
            subscriptionRevenueService.processAndDistributePremiumRevenue(previousMonthYear, false);
            log.info("Successfully completed Creator Premium Revenue payout for month: {}", previousMonthYear);
        } catch (Exception e) {
            log.error("Error processing Creator Premium Revenue payout for month: {}", previousMonthYear, e);
        }
    }
}