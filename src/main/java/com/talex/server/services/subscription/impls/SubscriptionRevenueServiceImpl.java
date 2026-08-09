package com.talex.server.services.subscription.impls;

import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.dtos.responses.creator.CreatorTierResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorConfig;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.enums.creator.RevenueTransactionType;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.records.CreatorRevenueData;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.repositories.subscription.SubscriptionRevenueLogRepository;
import com.talex.server.repositories.transaction.RevenueTransactionRepository;
import com.talex.server.services.creator.ICreatorConfigService;
import com.talex.server.services.creator.ICreatorService;
import com.talex.server.services.subscription.ISubscriptionRevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionRevenueServiceImpl implements ISubscriptionRevenueService {

    private final SubscriptionRevenueLogRepository subscriptionRevenueLogRepository;
    private final ICreatorService creatorService;
    private final ICreatorConfigService creatorConfigService;
    private final CreatorRepository creatorRepository;
    private final RevenueTransactionRepository revenueTransactionRepository;

    @Override
    @Transactional
    public List<RevenueTransaction> processAndDistributePremiumRevenue(String monthYear, boolean isDemo) {
        // 1. Lấy danh sách tổng doanh thu thô gom nhóm theo creatorId và subscriptionResultId trong tháng
        List<CreatorRevenueData> projections = subscriptionRevenueLogRepository.findAggregatedRevenueByMonthYear(monthYear);

        if (projections.isEmpty()) {
            log.info("Không tìm thấy dữ liệu revenue logs nào trong tháng {}", monthYear);
            return List.of();
        }

        // 2. Lấy CreatorConfig chứa tỉ lệ cơ bản (base_premium_share)
        CreatorConfig creatorConfig = creatorConfigService.getConfigEntity();
        double basePremiumShare = creatorConfig.getBasePremiumShare() != null ? creatorConfig.getBasePremiumShare() : 0.0;

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
            double effectiveRatio = Math.min(1.0, basePremiumShare + bonusRatio);
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
                            + " đạt được tổng là " + rawRevenue + "VND"
                            + " trừ phí nền tảng là " + (1 - effectiveRatio) * 100 + "%"
                            + " được tính từ 100 % trừ đi tỉ lệ " + creatorConfig.getBasePremiumShare() * 100 + "%"
                            + " và với cấp nhà sáng tạo là " + creatorDto.getCreatorTier().getTierName()
                            + " nên được bonus thêm " + creatorDto.getCreatorTier().getPremiumFundShareRatio() * 100 + "%"
                    )
                    .referenceType(ReferenceType.PREMIUM_RESULT)
                    .referenceId(subscriptionResultId)
                    .creator(creator)
                    .build();

            createdTransactions.add(transaction);
            log.info("Đã xử lý chia sẻ doanh thu cho creatorId: {}, rawRevenue: {}, ratio: {}, finalAmount: {} (isDemo: {})",
                    creatorId, rawRevenue, effectiveRatio, amountToAdd, isDemo);
        }

        // Chỉ lưu danh sách RevenueTransaction xuống DB nếu KHÔNG PHẢI là Demo
        if (!isDemo) {
            revenueTransactionRepository.saveAll(createdTransactions);
            log.info("Lưu thành công {} bản ghi RevenueTransaction vào DB.", createdTransactions.size());
        }

        return createdTransactions;
    }
}