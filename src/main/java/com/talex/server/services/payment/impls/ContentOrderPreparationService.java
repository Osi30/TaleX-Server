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

    public BigDecimal resolvePrice(UUID accountId, String itemType, String itemId) {
        return EpisodeOrderFulfillmentService.ITEM_TYPE.equals(itemType)
                ? resolveEpisodePrice(accountId, itemId)
                : resolveComboPrice(accountId, itemId);
    }

    private BigDecimal resolveEpisodePrice(UUID accountId, String episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Episode not found with id: " + episodeId));

        if (episode.getStatus() != EpisodeStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Episode not found with id: " + episodeId);
        }
        if (episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(accountId, episodeId)) {
            throw new PaymentException(PaymentErrorCode.CONTENT_ALREADY_OWNED);
        }

        return BigDecimal.valueOf(episode.getPriceVnd());
    }

    private BigDecimal resolveComboPrice(UUID accountId, String comboId) {
        ComboEpisode combo = comboEpisodeRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo not found with id: " + comboId));

        if (Boolean.TRUE.equals(combo.getIsDeleted())) {
            throw new ResourceNotFoundException("Combo not found with id: " + comboId);
        }

        boolean allEpisodesAlreadyOwned = !combo.getEpisodes().isEmpty() && combo.getEpisodes().stream()
                .allMatch(episode -> episodeUnlockedContentRepository
                        .existsByAccount_AccountIdAndEpisode_EpisodeId(accountId, episode.getEpisodeId()));
        if (allEpisodesAlreadyOwned) {
            throw new PaymentException(PaymentErrorCode.CONTENT_ALREADY_OWNED);
        }

        return BigDecimal.valueOf(combo.getPriceVnd());
    }
}
