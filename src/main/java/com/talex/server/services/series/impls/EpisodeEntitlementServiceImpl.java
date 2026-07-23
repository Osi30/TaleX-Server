package com.talex.server.services.series.impls;

import com.talex.server.entities.series.Episode;
import com.talex.server.entities.subscription.AccountSubscription;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.EpisodeUnlockType;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.series.EpisodeUnlockedContentRepository;
import com.talex.server.repositories.subscription.AccountSubscriptionRepository;
import com.talex.server.services.series.EpisodeEntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EpisodeEntitlementServiceImpl implements EpisodeEntitlementService {

    private final EpisodeRepository episodeRepository;
    private final EpisodeUnlockedContentRepository episodeUnlockedContentRepository;
    private final AccountSubscriptionRepository accountSubscriptionRepository;
    private final CreatorRepository creatorRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasPlaybackAccess(String viewerId, String episodeId) {
        Episode episode = episodeRepository.findById(episodeId).orElse(null);
        if (episode == null) {
            return true;
        }
        if (isFree(episode)) {
            return true;
        }

        UUID accountId = parseAccountId(viewerId);
        if (accountId == null) {
            return false;
        }

        return isOwner(accountId, episode.getCreatorId())
                || episodeUnlockedContentRepository.existsByAccount_AccountIdAndEpisode_EpisodeId(accountId, episodeId)
                || hasActiveSubscriptionEntitlement(accountId, episode);
    }

    private boolean isFree(Episode episode) {
        return episode.getUnlockType() == EpisodeUnlockType.FREE;
    }

    private boolean isOwner(UUID accountId, String episodeCreatorId) {
        if (episodeCreatorId == null) {
            return false;
        }
        return creatorRepository.findCreatorIdByAccountId(accountId)
                .map(episodeCreatorId::equals)
                .orElse(false);
    }

    private boolean hasActiveSubscriptionEntitlement(UUID accountId, Episode episode) {
        LocalDateTime now = LocalDateTime.now();
        return accountSubscriptionRepository
                .findFirstByAccount_AccountIdAndIsCancelledFalseAndStartTimeLessThanEqualAndEndTimeGreaterThanEqualOrderByEndTimeDesc(
                        accountId, now, now)
                .filter(subscription -> isUnlockedBySubscription(subscription, episode))
                .isPresent();
    }

    private boolean isUnlockedBySubscription(AccountSubscription subscription, Episode episode) {
        // Premium là quyền truy cập có thời hạn (không sở hữu vĩnh viễn), nên được mở khóa
        // toàn bộ nội dung trả phí kể cả tập nằm trong Combo — không như mua lẻ/mua Combo.
        return episode.getContentType() == ContentType.VIDEO
                ? Boolean.TRUE.equals(subscription.getIsMovieUnlocked())
                : Boolean.TRUE.equals(subscription.getIsStoryUnlocked());
    }

    private UUID parseAccountId(String viewerId) {
        if (viewerId == null || viewerId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(viewerId.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
