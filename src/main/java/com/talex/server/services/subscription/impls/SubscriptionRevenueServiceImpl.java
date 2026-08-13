package com.talex.server.services.subscription.impls;

import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.dtos.responses.creator.CreatorTierResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.enums.creator.RevenueTransactionType;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.records.CreatorRevenueData;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.repositories.subscription.SubscriptionRevenueLogRepository;
import com.talex.server.repositories.transaction.RevenueTransactionRepository;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.subscription.SubscriptionRevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionRevenueServiceImpl implements SubscriptionRevenueService {

    private final SubscriptionRevenueLogRepository subscriptionRevenueLogRepository;
    private final CreatorService creatorService;
    private final CreatorRepository creatorRepository;
    private final RevenueTransactionRepository revenueTransactionRepository;
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");


    @Override
    @Transactional
    public List<RevenueTransaction> processAndDistributePremiumRevenue(LocalDate monthYear, boolean isDemo) {
        // 1. Lấy danh sách tổng doanh thu thô gom nhóm theo creatorId và subscriptionResultId trong tháng
        String monthYearString = monthYear.format(MONTH_YEAR_FORMATTER);
        List<CreatorRevenueData> projections = subscriptionRevenueLogRepository.findAggregatedRevenueByMonthYear(monthYearString);

        if (projections.isEmpty()) {
            log.info("Không tìm thấy dữ liệu revenue logs nào trong tháng {}", monthYear);
            return List.of();
        }

        List<RevenueTransaction> createdTransactions = new ArrayList<>();

        // 3. Duyệt qua từng Creator để tính toán tỉ lệ và tạo giao dịch cộng tiền
        for (CreatorRevenueData projection : projections) {
            String creatorId = projection.creatorId();
            double rawRevenue = projection.totalRevenue() != null ? projection.totalRevenue() : 0.0;
            String subscriptionResultId = projection.subscriptionResultId();

            // Gọi ICreatorService lấy DTO thông qua getById
            CreatorResponseDto creatorDto = creatorService.getById(creatorId);

            // Lấy premiumFundShareRatio từ CreatorTierResponseDto (Bonus ratio)
            double bonusRatio = 0.0;
            if (creatorDto != null && creatorDto.getCreatorTier() != null) {
                CreatorTierResponseDto tier = creatorDto.getCreatorTier();
                if (tier.getPremiumFundShareRatio() != null) {
                    bonusRatio = tier.getPremiumFundShareRatio();
                }
            }

            // Tỉ lệ chia sẻ thực tế = min(1.0, basePremiumShare + bonusRatio)
            double effectiveRatio = 1.0 + bonusRatio;
            double finalRevenueAmount = rawRevenue * effectiveRatio;
            BigDecimal amountToAdd = BigDecimal.valueOf(finalRevenueAmount);

            // Lấy entity Creator để cộng dồn số dư
            Creator creator = creatorService.getEntityById(creatorId);

            BigDecimal balanceBefore = creator.getCurrentBalance() != null ? creator.getCurrentBalance() : BigDecimal.ZERO;
            BigDecimal balanceAfter = balanceBefore.add(amountToAdd);

            // Cập nhật currentBalance và totalBalance của Creator
            if (!isDemo) {
                BigDecimal currentTotal = creator.getTotalBalance() != null ? creator.getTotalBalance() : BigDecimal.ZERO;
                creator.setCurrentBalance(balanceAfter);
                creator.setTotalBalance(currentTotal.add(amountToAdd));

                creatorRepository.save(creator);
            }

            // Tạo đối tượng RevenueTransaction
            RevenueTransaction transaction = RevenueTransaction.builder()
                    .amount(amountToAdd)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .revenueTransactionType(RevenueTransactionType.PREMIUM_SHARE)
                    .description("Chia sẻ doanh thu Premium trong " + monthYear
                            + " đạt được " + rawRevenue + " VND"
                            + " và với cấp nhà sáng tạo là " + creatorDto.getCreatorTier().getTierLevel() + " - " + creatorDto.getCreatorTier().getTierName()
                            + " nên được cộng thêm " + creatorDto.getCreatorTier().getPremiumFundShareRatio() * 100 + "%"
                            + ", tổng cộng " + finalRevenueAmount + " VND"
                    )
                    .referenceType(ReferenceType.PREMIUM_RESULT)
                    .referenceId(subscriptionResultId)
                    .creator(creator)
                    .monthYear(monthYear)
                    .build();

            createdTransactions.add(transaction);
        }

        // Chỉ lưu danh sách RevenueTransaction xuống DB nếu KHÔNG PHẢI là Demo
        if (!isDemo) {
            revenueTransactionRepository.saveAll(createdTransactions);
        }

        return createdTransactions;
    }
}