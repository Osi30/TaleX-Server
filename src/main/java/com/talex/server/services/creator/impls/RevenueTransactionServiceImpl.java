package com.talex.server.services.creator.impls;

import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.dtos.responses.creator.CreatorTierResponseDto;
import com.talex.server.entities.config.CreatorConfig;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.EpisodeUnlockedContent;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueTransactionServiceImpl implements RevenueTransactionService {

    private final RevenueTransactionRepository revenueTransactionRepository;
    private final EpisodeService episodeService;
    private final CreatorService creatorService;
    private final CreatorConfigService creatorConfigService;

    @Override
    @Transactional
    public RevenueTransaction createFromEpisodeOrder(Order order, List<EpisodeUnlockedContent> unlockedContents) {
        // 1. Tính doanh thu NET = fiatAmount - vatAmount
        BigDecimal fiatAmount = order.getFiatAmount() != null ? order.getFiatAmount() : BigDecimal.ZERO;
        BigDecimal vatAmount = order.getVatAmount() != null ? order.getVatAmount() : BigDecimal.ZERO;
        BigDecimal netAmount = fiatAmount.subtract(vatAmount);

        // 2. Lấy creatorId từ episodeId (truy vấn qua episodeId từ itemId của order)
        String creatorId = episodeService.getCreatorIdByEpisodeId(order.getItemId());

        // 3. Lấy thông tin Creator & CreatorTier
        CreatorResponseDto creatorDto = creatorService.getById(creatorId);
        CreatorTierResponseDto tierDto = creatorDto.getCreatorTier();

        double directPurchaseShareRatio = (tierDto != null && tierDto.getDirectPurchaseShareRatio() != null)
                ? tierDto.getDirectPurchaseShareRatio()
                : 0.0;

        // 4. Lấy CreatorConfig
        CreatorConfig config = creatorConfigService.getConfigEntity();
        double baseUnlockShare = (config != null && config.getBaseUnlockShare() != null)
                ? config.getBaseUnlockShare()
                : 0.0;

        // 5. Tỷ lệ % nền tảng lấy = baseUnlockShare - directPurchaseShareRatio
        double platformShareRatioPercent = 1.0 - baseUnlockShare - directPurchaseShareRatio;
        if (platformShareRatioPercent < 0) {
            platformShareRatioPercent = 0.0;
        }

        // 6. Tính số tiền nền tảng lấy và creator thực nhận (làm tròn nguyên)
        BigDecimal platformAmount = netAmount.multiply(BigDecimal.valueOf(platformShareRatioPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        BigDecimal creatorAmount = netAmount.subtract(platformAmount).setScale(0, RoundingMode.HALF_UP);

        // 7. Xác định balanceBefore và balanceAfter
        BigDecimal balanceBefore = creatorDto.getCurrentBalance() != null ? creatorDto.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.add(creatorAmount);

        // 8. Tạo chuỗi mô tả chi tiết sản phẩm mua (Mua lẻ 1 tập hay Combo)
        String contentDetail = buildContentDetailDescription(unlockedContents);

        // Soạn description tổng thể
        assert tierDto != null;
        String description = String.format(
                "Số tiền nhận khi khách mua %s (Đơn hàng %s) tổng là %s VND sau khi trừ thuế (%s VND) là %s VNĐ, trừ nền tảng %s VNĐ (Tỷ lệ: %f%%) và cộng bonus cho cấp %s (%f%%) thì creator nhận được %s VNĐ.",
                contentDetail,
                order.getOrderId(),
                order.getTotalAmount().toString(),
                vatAmount,
                netAmount.toPlainString(),
                platformAmount.toPlainString(),
                platformShareRatioPercent * 100,
                tierDto.getTierLevel().toString(),
                tierDto.getDirectPurchaseShareRatio() * 100,
                creatorAmount.toPlainString()
        );

        // 9. Lấy Creator Entity để gắn khóa ngoại
        Creator creatorEntity = creatorService.getEntityById(creatorId);

        // 10. Tạo RevenueTransaction
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

        // 11. Cập nhật số dư Creator
        creatorService.updateBalance(creatorId, creatorAmount);

        return savedTransaction;
    }

    private String buildContentDetailDescription(List<EpisodeUnlockedContent> unlockedContents) {
        if (unlockedContents == null || unlockedContents.isEmpty()) {
            return "tập phim";
        }

        // Mua lẻ 1 tập
        if (unlockedContents.size() == 1) {
            Episode ep = unlockedContents.getFirst().getEpisode();
            String seriesTitle = getSeriesTitleFromEpisode(ep);
            return String.format("tập %d của series \"%s\"", ep.getEpisodeNumber(), seriesTitle);
        }

        // Gom nhóm tập theo Series
        Map<String, List<Episode>> episodesBySeries = unlockedContents.stream()
                .map(EpisodeUnlockedContent::getEpisode)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        this::getSeriesTitleFromEpisode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Nếu tất cả thuộc CÙNG 1 series
        if (episodesBySeries.size() == 1) {
            Map.Entry<String, List<Episode>> entry = episodesBySeries.entrySet().iterator().next();
            String seriesTitle = entry.getKey();
            String episodeNumbersStr = entry.getValue().stream()
                    .map(ep -> "tập " + ep.getEpisodeNumber())
                    .collect(Collectors.joining(", "));

            return String.format("combo gồm %s của series \"%s\"", episodeNumbersStr, seriesTitle);
        }

        // Nếu thuộc KHÁC series
        List<String> seriesParts = new ArrayList<>();
        for (Map.Entry<String, List<Episode>> entry : episodesBySeries.entrySet()) {
            String seriesTitle = entry.getKey();
            String epNums = entry.getValue().stream()
                    .map(ep -> "tập " + ep.getEpisodeNumber())
                    .collect(Collectors.joining(", "));

            seriesParts.add(String.format("%s của series \"%s\"", epNums, seriesTitle));
        }

        return "combo gồm " + String.join(" và ", seriesParts);
    }

    private String getSeriesTitleFromEpisode(Episode episode) {
        if (episode != null && episode.getSeason() != null && episode.getSeason().getSeries() != null) {
            return episode.getSeason().getSeries().getTitle();
        }
        return "N/A";
    }
}