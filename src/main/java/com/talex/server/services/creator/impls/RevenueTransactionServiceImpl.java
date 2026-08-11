package com.talex.server.services.creator.impls;

import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.dtos.responses.creator.CreatorTierResponseDto;
import com.talex.server.entities.config.CreatorConfig;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.creator.RevenueTransactionType;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.repositories.transaction.RevenueTransactionRepository;
import com.talex.server.services.config.CreatorConfigService;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.creator.RevenueTransactionService;
import com.talex.server.services.series.EpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class RevenueTransactionServiceImpl implements RevenueTransactionService {

    private final RevenueTransactionRepository revenueTransactionRepository;
    private final EpisodeService episodeService;
    private final CreatorService creatorService;
    private final CreatorConfigService creatorConfigService;

    @Override
    @Transactional
    public RevenueTransaction createFromEpisodeOrder(Order order) {
        // 1. Tính doanh thu NET = fiatAmount - vatAmount
        BigDecimal fiatAmount = order.getFiatAmount() != null ? order.getFiatAmount() : BigDecimal.ZERO;
        BigDecimal vatAmount = order.getVatAmount() != null ? order.getVatAmount() : BigDecimal.ZERO;
        BigDecimal netAmount = fiatAmount.subtract(vatAmount);

        // 2. Lấy creatorId từ episodeId nếu itemType là EPISODE
        String creatorId = episodeService.getCreatorIdByEpisodeId(order.getItemId());

        // 3. Lấy thông tin CreatorResponseDto & CreatorTier
        CreatorResponseDto creatorDto = creatorService.getById(creatorId);
        CreatorTierResponseDto tierDto = creatorDto.getCreatorTier();

        Double directPurchaseShareRatio = (tierDto != null && tierDto.getDirectPurchaseShareRatio() != null)
                ? tierDto.getDirectPurchaseShareRatio()
                : 0.0;

        // 4. Lấy CreatorConfig để lấy baseUnlockShare
        CreatorConfig config = creatorConfigService.getConfigEntity();
        Double baseUnlockShare = (config != null && config.getBaseUnlockShare() != null)
                ? config.getBaseUnlockShare()
                : 0.0;

        // 5. Tỷ lệ % nền tảng lấy = baseUnlockShare + (-directPurchaseShareRatio)
        double platformShareRatioPercent = baseUnlockShare - directPurchaseShareRatio;
        if (platformShareRatioPercent < 0) {
            platformShareRatioPercent = 0.0;
        }

        // 6. Tính số tiền nền tảng lấy và creator thực nhận (Làm tròn không lấy thập phân)
        BigDecimal platformAmount = netAmount.multiply(BigDecimal.valueOf(platformShareRatioPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        BigDecimal creatorAmount = netAmount.subtract(platformAmount).setScale(0, RoundingMode.HALF_UP);

        // 7. Xác định balanceBefore và balanceAfter
        BigDecimal balanceBefore = creatorDto.getCurrentBalance() != null ? creatorDto.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.add(creatorAmount);

        // 8. Soạn description chi tiết
        String description = String.format(
                "Khách mua tập phim (Đơn hàng %s). Doanh thu sau thuế (NET): %s VNĐ. " +
                        "Khấu trừ nền tảng: %s VNĐ (Tỷ lệ nền tảng: %.2f%% = Cơ bản %.2f%% - Bonus hạng %.2f%%). " +
                        "Creator thực nhận: %s VNĐ.",
                order.getOrderId(),
                netAmount.toPlainString(),
                platformAmount.toPlainString(),
                platformShareRatioPercent,
                baseUnlockShare,
                directPurchaseShareRatio,
                creatorAmount.toPlainString()
        );

        // 9. Lấy Creator Entity để tạo mối quan hệ FK
        Creator creatorEntity = creatorService.getEntityById(creatorId);

        // 10. Tạo object RevenueTransaction
        RevenueTransaction revenueTransaction = RevenueTransaction.builder()
                .amount(creatorAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .revenueTransactionType(RevenueTransactionType.CONTENT_SHARE)
                .description(description)
                .referenceType(ReferenceType.ORDER)
                .referenceId(order.getOrderId())
                .creator(creatorEntity)
                .build();

        RevenueTransaction savedTransaction = revenueTransactionRepository.save(revenueTransaction);

        // 11. Cập nhật currentBalance và totalBalance của Creator
        creatorService.updateBalance(creatorId, creatorAmount);

        return savedTransaction;
    }
}