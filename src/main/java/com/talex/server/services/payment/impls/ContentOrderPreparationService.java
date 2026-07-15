package com.talex.server.services.payment.impls;

import com.talex.server.entities.series.ComboEpisode;
import com.talex.server.entities.series.Episode;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.exceptions.codes.PaymentErrorCode;
import com.talex.server.exceptions.details.PaymentException;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.series.ComboEpisodeRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.series.EpisodeUnlockedContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContentOrderPreparationService {

    private final EpisodeRepository episodeRepository;
    private final ComboEpisodeRepository comboEpisodeRepository;
    private final EpisodeUnlockedContentRepository episodeUnlockedContentRepository;

    public String normalizeItemType(String rawItemType) {
        String upper = rawItemType == null ? "" : rawItemType.toUpperCase();
        if (!EpisodeOrderFulfillmentService.ITEM_TYPE.equals(upper) && !ComboOrderFulfillmentService.ITEM_TYPE.equals(upper)) {
            throw new PaymentException(PaymentErrorCode.INVALID_ITEM_TYPE);
        }
        return upper;
    }

    public ContentPriceResolution resolvePrice(UUID accountId, String itemType, String itemId) {
        return EpisodeOrderFulfillmentService.ITEM_TYPE.equals(itemType)
                ? resolveEpisodePrice(accountId, itemId)
                : resolveComboPrice(accountId, itemId);
    }

    private ContentPriceResolution resolveEpisodePrice(UUID accountId, String episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episode not found with id: " + episodeId));

        if (episode.getStatus() != EpisodeStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Episode not found with id: " + episodeId);
        }
        if (episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(accountId, episodeId)) {
            throw new PaymentException(PaymentErrorCode.CONTENT_ALREADY_OWNED);
        }

        BigDecimal price = BigDecimal.valueOf(episode.getPriceVnd());
        return new ContentPriceResolution(price, price, 0, 0);
    }

    /**
     * Giá combo được set cố định bởi creator (không tự tính = tổng giá lẻ), nên nếu user đã
     * sở hữu một phần số tập trong combo, phần đã sở hữu được trừ theo ĐƠN GIÁ QUY ĐỔI TỪ
     * GIÁ COMBO (giá combo / tổng số tập) — không hoàn theo giá lẻ đã mua trước đó. Cách này
     * đảm bảo user không bị tính tiền 2 lần cho phần đã sở hữu, đồng thời web vẫn giữ được
     * biên lợi nhuận vì đơn giá quy đổi combo thường thấp hơn giá mua lẻ từng tập.
     */
    private ContentPriceResolution resolveComboPrice(UUID accountId, String comboId) {
        ComboEpisode combo = comboEpisodeRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo not found with id: " + comboId));

        if (Boolean.TRUE.equals(combo.getIsDeleted())) {
            throw new ResourceNotFoundException("Combo not found with id: " + comboId);
        }

        List<Episode> episodes = combo.getEpisodes();
        BigDecimal originalPrice = BigDecimal.valueOf(combo.getPriceVnd());
        int totalEpisodeCount = episodes.size();

        if (episodes.isEmpty()) {
            return new ContentPriceResolution(originalPrice, originalPrice, 0, 0);
        }

        long ownedEpisodeCount = episodes.stream()
                .filter(episode -> episodeUnlockedContentRepository
                        .existsByAccount_AccountIdAndEpisode_EpisodeId(accountId, episode.getEpisodeId()))
                .count();

        if (ownedEpisodeCount == totalEpisodeCount) {
            throw new PaymentException(PaymentErrorCode.CONTENT_ALREADY_OWNED);
        }
        if (ownedEpisodeCount == 0) {
            return new ContentPriceResolution(originalPrice, originalPrice, 0, totalEpisodeCount);
        }

        BigDecimal perEpisodeComboRate = originalPrice.divide(
                BigDecimal.valueOf(totalEpisodeCount), 0, RoundingMode.FLOOR);
        BigDecimal ownershipDiscount = perEpisodeComboRate.multiply(BigDecimal.valueOf(ownedEpisodeCount));
        BigDecimal payablePrice = originalPrice.subtract(ownershipDiscount).max(BigDecimal.ZERO);

        return new ContentPriceResolution(payablePrice, originalPrice, (int) ownedEpisodeCount, totalEpisodeCount);
    }

    /**
     * @param payablePrice       Số tiền thực tế phải trả (đã trừ phần sở hữu 1 phần nếu là combo)
     * @param originalPrice      Giá gốc trước khi trừ (bằng payablePrice nếu không có giảm giá)
     * @param ownedEpisodeCount  Số tập trong combo đã sở hữu trước đó (0 nếu mua lẻ hoặc combo mới)
     * @param totalEpisodeCount  Tổng số tập trong combo (0 nếu mua lẻ)
     */
    public record ContentPriceResolution(
            BigDecimal payablePrice,
            BigDecimal originalPrice,
            int ownedEpisodeCount,
            int totalEpisodeCount) {
    }
}
