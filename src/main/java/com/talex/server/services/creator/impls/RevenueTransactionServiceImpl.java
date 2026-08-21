package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.dtos.responses.creator.CreatorTierResponseDto;
import com.talex.server.dtos.revenue.response.RevenueSummaryResponseDto;
import com.talex.server.dtos.revenue.response.RevenueTimeSeriesResponseDto;
import com.talex.server.dtos.revenue.response.RevenueTransactionDto;
import com.talex.server.dtos.settlement.UnsettledEpisodeRevenueDto;
import com.talex.server.dtos.settlement.UnsettledEpisodeSubscriptionRevenueDto;
import com.talex.server.entities.config.CreatorConfig;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.EpisodeUnlockedContent;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.creator.RevenueTransactionType;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.mappers.settlement.RevenueTransactionMapper;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.subscription.SubscriptionRevenueLogRepository;
import com.talex.server.repositories.transaction.RevenueTransactionRepository;
import com.talex.server.services.config.CreatorConfigService;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.creator.RevenueTransactionService;
import com.talex.server.services.series.EpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueTransactionServiceImpl implements RevenueTransactionService {

    private final RevenueTransactionMapper revenueTransactionMapper;
    private final RevenueTransactionRepository revenueTransactionRepository;
    private final EpisodeService episodeService;
    private final CreatorService creatorService;
    private final CreatorConfigService creatorConfigService;
    private final EpisodeRepository episodeRepository;
    private final SubscriptionRevenueLogRepository subscriptionRevenueLogRepository;

    @Override
    @Transactional
    public RevenueTransaction createFromEpisodeOrder(Order order, List<EpisodeUnlockedContent> unlockedContents) {
        // 1. Tính doanh thu NET = totalAmount - vatAmount
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal vatAmount = order.getVatAmount() != null ? order.getVatAmount() : BigDecimal.ZERO;
        BigDecimal netAmount = totalAmount.subtract(vatAmount);

        // 2. Lấy creatorId từ episodeId
        String creatorId = episodeService.getCreatorIdByEpisodeId(order.getItemId());

        // 3. Lấy thông tin Creator & CreatorTier (Ratio dạng thập phân 0.0 -> 1.0)
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

        // 5. Tỷ lệ nền tảng thu = baseUnlockShare - directPurchaseShareRatio (Dạng thập phân từ 0.0 -> 1.0)
        double platformShareRatio = baseUnlockShare - directPurchaseShareRatio;
        if (platformShareRatio < 0) {
            directPurchaseShareRatio = platformShareRatio;
            platformShareRatio = 0.0;
        }

        // 6. Tính số tiền nền tảng giữ & creator nhận
        BigDecimal platformAmount = netAmount.multiply(BigDecimal.valueOf(platformShareRatio))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal creatorAmount = netAmount.subtract(platformAmount).setScale(0, RoundingMode.HALF_UP);

        // 7. Số dư trước và sau
        BigDecimal balanceBefore = creatorDto.getCurrentBalance() != null ? creatorDto.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.add(creatorAmount);

        // 8. Tên chi tiết các tập đã mua
        String contentDetail = buildContentDetailDescription(unlockedContents);

        // Biến phục vụ hiển thị phần trăm (nhân 100 chỉ khi format ra chuỗi)
        double platformPercentForDisplay = platformShareRatio * 100;
        double bonusPercentForDisplay = directPurchaseShareRatio * 100;
        String tierLevelStr = (tierDto != null && tierDto.getTierLevel() != null) ? tierDto.getTierLevel().toString() : "0";

        String description = String.format(
                "Số tiền nhận khi khách mua %s (Đơn hàng %s) tổng là %s VND sau khi trừ thuế (%s VND) là %s VNĐ, trừ nền tảng %s VNĐ (Tỷ lệ: %.2f%%) và cộng bonus cho cấp %s (%.2f%%) thì creator nhận được %s VNĐ.",
                contentDetail,
                order.getOrderId(),
                totalAmount.toPlainString(),
                vatAmount.toPlainString(),
                netAmount.toPlainString(),
                platformAmount.toPlainString(),
                platformPercentForDisplay,
                tierLevelStr,
                bonusPercentForDisplay,
                creatorAmount.toPlainString()
        );

        // 9. Lấy Creator Entity
        Creator creatorEntity = creatorService.getEntityById(creatorId);

        // 10. Lưu RevenueTransaction
        RevenueTransaction revenueTransaction = RevenueTransaction.builder()
                .amount(creatorAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .revenueTransactionType(RevenueTransactionType.CONTENT_SHARE)
                .description(description)
                .referenceType(ReferenceType.ORDER)
                .referenceId(order.getOrderId())
                .creator(creatorEntity)
                .monthYear(LocalDate.now())
                .build();

        RevenueTransaction savedTransaction = revenueTransactionRepository.save(revenueTransaction);

        // 11. Cập nhật số dư Creator
        creatorService.updateBalance(creatorId, creatorAmount);

        return savedTransaction;
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<RevenueTransactionDto> getAllTransactions(String creatorId, int page, int pageSize) {
        int validPage = Math.max(page, 1);
        int validPageSize = pageSize < 1 ? 20 : pageSize;
        Pageable pageable = PageRequest.of(validPage - 1, validPageSize);

        Page<RevenueTransaction> pageResult;
        if (creatorId != null && !creatorId.trim().isEmpty()) {
            pageResult = revenueTransactionRepository.findByCreator_CreatorIdOrderByCreatedAtDesc(creatorId, pageable);
        } else {
            pageResult = revenueTransactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<RevenueTransactionDto> content = pageResult.stream()
                .map(revenueTransactionMapper::toDto)
                .toList();

        return BasePageResponse.<RevenueTransactionDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueSummaryResponseDto getRevenueSummary(String creatorId, LocalDateTime startDate, LocalDateTime endDate) {
        List<RevenueTransaction> transactions = fetchTransactionsBetween(creatorId, startDate, endDate);

        Map<RevenueTransactionType, BigDecimal> amountByType = new EnumMap<>(RevenueTransactionType.class);
        for (RevenueTransactionType type : RevenueTransactionType.values()) {
            amountByType.put(type, BigDecimal.ZERO);
        }

        for (RevenueTransaction tx : transactions) {
            RevenueTransactionType type = tx.getRevenueTransactionType();
            if (type != null) {
                BigDecimal current = amountByType.getOrDefault(type, BigDecimal.ZERO);
                BigDecimal txAmount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                amountByType.put(type, current.add(txAmount));
            }
        }

        BigDecimal premiumShare = amountByType.getOrDefault(RevenueTransactionType.PREMIUM_SHARE, BigDecimal.ZERO);
        BigDecimal contentShare = amountByType.getOrDefault(RevenueTransactionType.CONTENT_SHARE, BigDecimal.ZERO);
        BigDecimal totalRevenue = premiumShare.add(contentShare);

        BigDecimal totalPenalty = amountByType.getOrDefault(RevenueTransactionType.PENALTY_DEDUCTION, BigDecimal.ZERO);
        BigDecimal totalAdjustment = amountByType.getOrDefault(RevenueTransactionType.ADJUSTMENT, BigDecimal.ZERO);

        return RevenueSummaryResponseDto.builder()
                .totalRevenueAmount(totalRevenue)
                .totalPenaltyAmount(totalPenalty)
                .totalAdjustmentAmount(totalAdjustment)
                .amountByType(amountByType)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueTimeSeriesResponseDto> getRevenueTimeSeries(String creatorId, LocalDateTime startDate, LocalDateTime endDate) {
        List<RevenueTransaction> transactions = fetchTransactionsBetween(creatorId, startDate, endDate);

        long daysDifference = Duration.between(startDate, endDate).toDays();

        String groupUnit;
        DateTimeFormatter formatter;

        if (daysDifference < 7) {
            groupUnit = "HOUR";
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
        } else if (daysDifference < 30) {
            groupUnit = "DAY";
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        } else if (daysDifference < 365) {
            groupUnit = "MONTH";
            formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        } else {
            groupUnit = "YEAR";
            formatter = DateTimeFormatter.ofPattern("yyyy");
        }

        Map<String, List<RevenueTransaction>> groupedMap = transactions.stream()
                .filter(tx -> tx.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        tx -> tx.getCreatedAt().format(formatter),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<RevenueTimeSeriesResponseDto> result = new ArrayList<>();
        for (Map.Entry<String, List<RevenueTransaction>> entry : groupedMap.entrySet()) {
            String timePeriod = entry.getKey();
            List<RevenueTransaction> txList = entry.getValue();

            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalPenalty = BigDecimal.ZERO;
            BigDecimal totalAdjustment = BigDecimal.ZERO;

            for (RevenueTransaction tx : txList) {
                BigDecimal txAmount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                RevenueTransactionType type = tx.getRevenueTransactionType();

                if (type == RevenueTransactionType.PREMIUM_SHARE || type == RevenueTransactionType.CONTENT_SHARE) {
                    totalRevenue = totalRevenue.add(txAmount);
                } else if (type == RevenueTransactionType.PENALTY_DEDUCTION) {
                    totalPenalty = totalPenalty.add(txAmount);
                } else if (type == RevenueTransactionType.ADJUSTMENT) {
                    totalAdjustment = totalAdjustment.add(txAmount);
                }
            }

            result.add(RevenueTimeSeriesResponseDto.builder()
                    .timePeriod(timePeriod)
                    .totalRevenueAmount(totalRevenue)
                    .totalPenaltyAmount(totalPenalty)
                    .totalAdjustmentAmount(totalAdjustment)
                    .groupUnit(groupUnit)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public UnsettledEpisodeRevenueDto getUnsettledRevenueByEpisodeId(String episodeId) {
        // Kiểm tra tồn tại của Episode
        boolean exists = episodeRepository.existsById(episodeId);
        if (!exists) {
            throw new IllegalArgumentException("Không tìm thấy Episode với ID: " + episodeId);
        }

        BigDecimal totalUnsettled = revenueTransactionRepository.calculateUnsettledRevenueByEpisodeId(episodeId);

        return UnsettledEpisodeRevenueDto.builder()
                .episodeId(episodeId)
                .unsettledAmount(totalUnsettled)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UnsettledEpisodeSubscriptionRevenueDto getUnsettledSubscriptionRevenueByEpisodeId(String episodeId) {
        boolean exists = episodeRepository.existsById(episodeId);
        if (!exists) {
            throw new IllegalArgumentException("Không tìm thấy Episode với ID: " + episodeId);
        }

        BigDecimal totalUnsettled = subscriptionRevenueLogRepository.calculateUnsettledSubscriptionRevenueByEpisodeId(episodeId);

        return UnsettledEpisodeSubscriptionRevenueDto.builder()
                .episodeId(episodeId)
                .unsettledAmount(totalUnsettled)
                .build();
    }

    private String buildContentDetailDescription(List<EpisodeUnlockedContent> unlockedContents) {
        if (unlockedContents == null || unlockedContents.isEmpty()) {
            return "tập phim";
        }

        if (unlockedContents.size() == 1) {
            Episode ep = unlockedContents.getFirst().getEpisode();
            String seriesTitle = getSeriesTitleFromEpisode(ep);
            return String.format("tập %d của series \"%s\"", ep.getEpisodeNumber(), seriesTitle);
        }

        Map<String, List<Episode>> episodesBySeries = unlockedContents.stream()
                .map(EpisodeUnlockedContent::getEpisode)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        this::getSeriesTitleFromEpisode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        if (episodesBySeries.size() == 1) {
            Map.Entry<String, List<Episode>> entry = episodesBySeries.entrySet().iterator().next();
            String seriesTitle = entry.getKey();
            String episodeNumbersStr = entry.getValue().stream()
                    .map(ep -> "tập " + ep.getEpisodeNumber())
                    .collect(Collectors.joining(", "));

            return String.format("combo gồm %s của series \"%s\"", episodeNumbersStr, seriesTitle);
        }

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

    private List<RevenueTransaction> fetchTransactionsBetween(String creatorId, LocalDateTime startDate, LocalDateTime endDate) {
        if (creatorId != null && !creatorId.trim().isEmpty()) {
            return revenueTransactionRepository.findByCreator_CreatorIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                    creatorId, startDate, endDate);
        }
        return revenueTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(startDate, endDate);
    }
}