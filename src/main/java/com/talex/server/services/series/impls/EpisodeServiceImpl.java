package com.talex.server.services.series.impls;

import com.talex.server.dtos.analytic.EpisodeLogResponseDto;
import com.talex.server.dtos.requests.series.EpisodeRequestDto;
import com.talex.server.dtos.requests.series.EpisodeUnlockSettingsRequestDto;
import com.talex.server.dtos.responses.series.EpisodeRefs;
import com.talex.server.dtos.responses.series.EpisodeResponseDto;
import com.talex.server.entities.Notification;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.EpisodeLog;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.NotificationType;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.enums.series.*;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.NotificationRepository;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.repositories.series.CategoryRepository;
import com.talex.server.repositories.series.EpisodeLogRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.series.TagRepository;
import com.talex.server.services.audit.ContentAuditLogger;
import com.talex.server.services.media.impls.ContentOwnershipService;
import com.talex.server.services.series.EpisodeService;
import com.talex.server.services.series.SeasonService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EpisodeServiceImpl implements EpisodeService {
    private static final long CREATOR_ROLE_ID = 2L;

    private final EpisodeRepository episodeRepository;
    private final MediaRepository mediaRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final EpisodeLogRepository episodeLogRepository;
    private final SeasonService seasonService;
    private final NotificationRepository notificationRepository;
    private final ContentOwnershipService contentOwnershipService;
    private final ContentAuditLogger contentAuditLogger;
    private final ContentCascadeDeleteHelper contentCascadeDeleteHelper;

    @Transactional
    @Override
    public EpisodeResponseDto create(String seasonId, EpisodeRequestDto request, String accountId) {
        Season season = seasonService.findActiveEntity(seasonId);
        contentOwnershipService.assertCanManage(season.getSeries(), accountId);
        ContentType contentType = request.getContentType() != null
                ? request.getContentType()
                : season.getSeries().getContentType();
        validateEpisodeContentType(season, contentType);

        Episode episode = new Episode();
        episode.setSeason(season);
        episode.setCreatorId(season.getSeries().getCreator().getCreatorId());
        episode.setEpisodeNumber(request.getEpisodeNumber() != null
                ? request.getEpisodeNumber()
                : nextEpisodeNumber(seasonId));
        episode.setTitle(request.getTitle());
        episode.setDescription(request.getDescription());
        episode.setThumbnail(request.getThumbnail());
        episode.setContentType(contentType);
        episode.setStatus(EpisodeStatus.DRAFT);
        episode.setReleasedUpdateTime(LocalDateTime.now());
        episode.setScheduledPublishAt(null);
        episode.setTotalPage(request.getTotalPage());
        applyFreeUnlockSettings(episode);
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "CREATE", accountId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional(readOnly = true)
    @Override
    public EpisodeResponseDto getById(String id, String accountId) {
        Episode episode = findActiveEntity(id);
        contentOwnershipService.assertCanView(episode, accountId);
        return toResponse(episode);
    }

    @Transactional(readOnly = true)
    @Override
    public EpisodeResponseDto getPublicById(String id) {
        return toResponse(findPublicEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<EpisodeResponseDto> listBySeason(String seasonId, String accountId) {
        Season season = seasonService.findActiveEntity(seasonId);
        contentOwnershipService.assertCanView(season, accountId);
        return episodeRepository.findAllBySeason_SeasonIdAndIsDeletedFalseOrderByEpisodeNumberAsc(seasonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<EpisodeResponseDto> listPublicBySeason(String seasonId) {
        seasonService.findPublicEntity(seasonId);
        return episodeRepository
                .findAllBySeason_SeasonIdAndStatusInAndIsDeletedFalseOrderByEpisodeNumberAsc(
                        seasonId,
                        List.of(EpisodeStatus.PUBLISHED, EpisodeStatus.SCHEDULED))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(
            value = "episode_refs",
            key = "#id",
            cacheManager = "redisCacheManager"
    )
    @Override
    public EpisodeResponseDto update(String id, EpisodeRequestDto request, String accountId) {
        Episode episode = findManageableEntity(id, accountId);
        if (request.getEpisodeNumber() != null) {
            episode.setEpisodeNumber(request.getEpisodeNumber());
        }
        episode.setTitle(request.getTitle());
        episode.setDescription(request.getDescription());
        episode.setThumbnail(request.getThumbnail());
        if (request.getContentType() != null) {
            validateEpisodeContentType(episode.getSeason(), request.getContentType());
            episode.setContentType(request.getContentType());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() == EpisodeStatus.SCHEDULED) {
                throw ContentModuleException.badRequest("Use the schedule-publish endpoint to schedule an episode");
            }
            if (request.getStatus() == EpisodeStatus.PUBLISHED) {
                ensureReadyMediaForPublish(episode);
                publishParentsImmediately(episode, accountId);
                episode.setScheduledPublishAt(null);
                if (episode.getPublishedAt() == null) {
                    episode.setPublishedAt(LocalDateTime.now());
                }
            } else if (request.getStatus() == EpisodeStatus.SCHEDULED) {
                throw ContentModuleException.badRequest("Cannot set status to SCHEDULED directly via update. Use schedule endpoints.");
            } else if (episode.getStatus() == EpisodeStatus.SCHEDULED) {
                cancelScheduledPublication(episode, accountId);
            }
            if (episode.getStatus() != request.getStatus()) {
                episode.setStatus(request.getStatus());
                episode.setReleasedUpdateTime(LocalDateTime.now());
            }
        }
        episode.setTotalPage(request.getTotalPage());
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "UPDATE", accountId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional
    @Override
    public EpisodeResponseDto updateUnlockSettings(String id, EpisodeUnlockSettingsRequestDto request, String accountId) {
        Episode episode = findManageableEntity(id, accountId);
        ensureAccountHasCreatorRole(accountId);
        applyUnlockSettings(episode, request);
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "UPDATE_UNLOCK_SETTINGS", accountId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional
    @Override
    public EpisodeResponseDto schedulePublish(String id, LocalDateTime scheduledPublishAt, String actorId) {
        Episode episode = findManageableEntity(id, actorId);
        ensureScheduledPublishAt(scheduledPublishAt);
        ensureEpisodeCanBeScheduled(episode);
        ensureReadyMediaForPublish(episode);

        prepareParentsForSchedule(episode, actorId);
        episode.setStatus(EpisodeStatus.SCHEDULED);
        episode.setReleasedUpdateTime(LocalDateTime.now());
        episode.setScheduledPublishAt(scheduledPublishAt);
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "SCHEDULE", actorId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional
    @Override
    public EpisodeResponseDto cancelSchedule(String id, String actorId) {
        Episode episode = findManageableEntity(id, actorId);
        if (episode.getStatus() != EpisodeStatus.SCHEDULED) {
            throw ContentModuleException.badRequest("Episode is not scheduled");
        }
        cancelScheduledPublication(episode, actorId);
        episode.setStatus(EpisodeStatus.DRAFT);
        episode.setReleasedUpdateTime(LocalDateTime.now());
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "CANCEL_SCHEDULE", actorId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional
    @Override
    public EpisodeResponseDto publish(String id, String actorId) {
        Episode episode = findManageableEntity(id, actorId);
        // Episode bị Admin ép ẩn chỉ được gỡ qua forceUnhide() (thao tác riêng của Admin) —
        // nếu không chặn ở đây, Creator có thể tự bấm "Xuất bản Ngay" để tự vô hiệu hoá
        // quyết định ép ẩn của Admin.
        if (episode.getStatus() == EpisodeStatus.FORCE_HIDDEN) {
            throw ContentModuleException.badRequest("Episode is force-hidden by admin and cannot be republished by the creator");
        }
        ensureReadyMediaForPublish(episode);
        publishParentsImmediately(episode, actorId);

        episode.setStatus(EpisodeStatus.PUBLISHED);
        episode.setReleasedUpdateTime(LocalDateTime.now());
        episode.setScheduledPublishAt(null);
        if (episode.getPublishedAt() == null) {
            episode.setPublishedAt(LocalDateTime.now());
        }
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "PUBLISH", actorId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional
    @Override
    public EpisodeResponseDto publishScheduled(String id, String actorId) {
        Episode episode = lockActiveEntity(id);
        ensureScheduledEpisodeIsDue(episode);
        ensureReadyMediaForPublish(episode);

        publishScheduledParents(episode, actorId);
        episode.setStatus(EpisodeStatus.PUBLISHED);
        episode.setReleasedUpdateTime(LocalDateTime.now());
        episode.setScheduledPublishAt(null);
        if (episode.getPublishedAt() == null) {
            episode.setPublishedAt(LocalDateTime.now());
        }
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "PUBLISH_SCHEDULED", actorId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional
    @Override
    public EpisodeResponseDto hide(String id, String actorId) {
        Episode episode = findManageableEntity(id, actorId);
        if (episode.getStatus() == EpisodeStatus.SCHEDULED) {
            cancelScheduledPublication(episode, actorId);
        }
        episode.setStatus(EpisodeStatus.HIDDEN);
        episode.setReleasedUpdateTime(LocalDateTime.now());

        hideParentsIfNoOtherPublished(episode);

        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "HIDE", actorId, episode.getCreatorId());
        return toResponse(episode);
    }

    private void hideParentsIfNoOtherPublished(Episode episode) {
        Season season = episode.getSeason();
        long publishedEpisodesInSeason = episodeRepository.countBySeasonIdExcludingEpisodeAndStatus(
                season.getSeasonId(), episode.getEpisodeId(), EpisodeStatus.PUBLISHED);

        if (publishedEpisodesInSeason == 0 && season.getStatus() == SeasonStatus.PUBLISHED) {
            long scheduledEpisodesInSeason = episodeRepository.countBySeasonIdExcludingEpisodeAndStatus(
                    season.getSeasonId(), episode.getEpisodeId(), EpisodeStatus.SCHEDULED);
            if (scheduledEpisodesInSeason > 0) {
                season.setStatus(SeasonStatus.SCHEDULED);
                season.setReleasedUpdateTime(LocalDateTime.now());
            } else {
                season.setStatus(SeasonStatus.HIDDEN);
                season.setReleasedUpdateTime(LocalDateTime.now());
            }
        }

        Series series = season.getSeries();
        long publishedEpisodesInSeries = episodeRepository.countBySeriesIdExcludingEpisodeAndStatus(
                series.getSeriesId(), episode.getEpisodeId(), EpisodeStatus.PUBLISHED);

        if (publishedEpisodesInSeries == 0 && series.getStatus() == SeriesStatus.PUBLISHED) {
            long scheduledEpisodesInSeries = episodeRepository.countBySeriesIdExcludingEpisodeAndStatus(
                    series.getSeriesId(), episode.getEpisodeId(), EpisodeStatus.SCHEDULED);
            if (scheduledEpisodesInSeries > 0) {
                series.setStatus(SeriesStatus.SCHEDULED);
                series.setReleasedUpdateTime(LocalDateTime.now());
            } else {
                series.setStatus(SeriesStatus.HIDDEN);
                series.setReleasedUpdateTime(LocalDateTime.now());
            }
        }
    }

    @Transactional
    @Override
    public EpisodeResponseDto forceHide(String id, String actorId) {
        Episode episode = findActiveEntity(id);
        episode.setStatus(EpisodeStatus.FORCE_HIDDEN);
        episode.setReleasedUpdateTime(LocalDateTime.now());
        Episode saved = episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", saved.getEpisodeId(), "FORCE_HIDE", actorId, episode.getCreatorId());
        notifyCreatorOfVisibilityChange(saved, NotificationType.EPISODE_FORCE_HIDDEN,
                "Tập nội dung tạm ngừng hiển thị công khai",
                "Tập \"" + saved.getTitle() + "\" đã được duyệt trước đó nhưng hiện đang tạm ngừng hiển thị công khai theo quyết định của đội ngũ quản trị. Vui lòng liên hệ bộ phận hỗ trợ để được giải thích chi tiết.");
        return toResponse(saved);
    }

    @Transactional
    @Override
    public EpisodeResponseDto forceUnhide(String id, String actorId) {
        Episode episode = findActiveEntity(id);
        if (episode.getStatus() != EpisodeStatus.FORCE_HIDDEN) {
            throw ContentModuleException.badRequest("Episode is not force-hidden");
        }
        // Khôi phục thẳng về PUBLISHED (khác forceUnhide ở Media — quyết định nghiệp vụ:
        // episode này đã từng công khai bình thường trước khi bị ép ẩn, Admin gỡ ép ẩn
        // nghĩa là xác nhận cho hiển thị lại ngay, không cần Creator phải tự xuất bản lại).
        episode.setStatus(EpisodeStatus.PUBLISHED);
        episode.setReleasedUpdateTime(LocalDateTime.now());
        Episode saved = episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", saved.getEpisodeId(), "FORCE_UNHIDE", actorId, episode.getCreatorId());
        notifyCreatorOfVisibilityChange(saved, NotificationType.EPISODE_RESTORED,
                "Tập nội dung đã được khôi phục hiển thị",
                "Tập \"" + saved.getTitle() + "\" đã được khôi phục hiển thị công khai và đang ở trạng thái Đã xuất bản trở lại.");
        return toResponse(saved);
    }

    // Notification.recipientId là accountId (xem NotificationController), còn
    // episode.getCreatorId() là Creator entity id (khác accountId) — phải đi qua chuỗi
    // Season -> Series -> Creator -> Account để lấy đúng accountId, cùng cách
    // ModerationServiceImpl.resolveOwnerUserId() đã làm cho case EPISODE.
    private void notifyCreatorOfVisibilityChange(Episode episode, NotificationType type, String title, String content) {
        String recipientAccountId = episode.getSeason().getSeries().getCreator().getAccount().getAccountId().toString();
        notificationRepository.save(Notification.builder()
                .recipientId(recipientAccountId)
                .title(title)
                .content(content)
                .type(type)
                .referenceType("EPISODE")
                .referenceId(episode.getEpisodeId())
                .build());
    }

    @Transactional
    @Override
    public EpisodeResponseDto unhide(String id, String actorId) {
        Episode episode = findActiveEntity(id);
        contentOwnershipService.assertCanManage(episode, actorId);
        ensureReadyMediaForPublish(episode);
        publishParentsImmediately(episode, actorId);
        episode.setStatus(EpisodeStatus.PUBLISHED);
        episode.setReleasedUpdateTime(LocalDateTime.now());
        episode.setScheduledPublishAt(null);
        if (episode.getPublishedAt() == null) {
            episode.setPublishedAt(LocalDateTime.now());
        }
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "UNHIDE", actorId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Episode episode = findManageableEntity(id, actorId);
        if (episode.getStatus() == EpisodeStatus.SCHEDULED) {
            cancelScheduledPublication(episode, actorId);
        }
        episode.setStatus(EpisodeStatus.DELETED);
        episode.setReleasedUpdateTime(LocalDateTime.now());
        episode.softDelete();
        episodeRepository.save(episode);
        contentCascadeDeleteHelper.cascadeDeleteEpisodeMedia(episode.getEpisodeId(), actorId);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "DELETE", actorId, episode.getCreatorId());
    }

    @Override
    public Episode findActiveEntity(String id) {
        return episodeRepository.findByEpisodeIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Episode not found: " + id));
    }

    private Episode lockActiveEntity(String id) {
        return episodeRepository.lockByEpisodeIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Episode not found: " + id));
    }

    private Episode findManageableEntity(String id, String accountId) {
        Episode episode = findActiveEntity(id);
        contentOwnershipService.assertCanManage(episode, accountId);
        return episode;
    }

    @Override
    public Episode findPublicEntity(String id) {
        Episode episode = findActiveEntity(id);
        if (episode.getStatus() != EpisodeStatus.PUBLISHED && episode.getStatus() != EpisodeStatus.SCHEDULED) {
            throw ContentModuleException.notFound("Public episode not found: " + id);
        }
        Season season = episode.getSeason();
        if (season.getStatus() != SeasonStatus.PUBLISHED && season.getStatus() != SeasonStatus.SCHEDULED) {
            throw ContentModuleException.notFound("Public season not found: " + season.getSeasonId());
        }
        Series series = season.getSeries();
        if (series.getStatus() != SeriesStatus.PUBLISHED && series.getStatus() != SeriesStatus.SCHEDULED) {
            throw ContentModuleException.notFound("Public series not found: " + series.getSeriesId());
        }
        return episode;
    }

    @Cacheable(
            value = "episode_series",
            key = "#episodeId",
            cacheManager = "redisCacheManager",
            unless = "#result == null"
    )
    @Override
    public String getSeriesIdByEpisodeId(String episodeId) {
        return episodeRepository.findSeriesIdByEpisodeId(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Series cho Episode ID: " + episodeId));
    }

    @Cacheable(
            value = "episode_creator",
            key = "#episodeId",
            cacheManager = "redisCacheManager",
            unless = "#result == null"
    )
    @Override
    public String getCreatorIdByEpisodeId(String episodeId) {
        return episodeRepository.getCreatorIdByEpisodeId(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Creator cho Episode ID: " + episodeId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "episode_refs",
            key = "#episodeId",
            cacheManager = "redisCacheManager",
            unless = "#result == null"
    )
    public EpisodeRefs getEpisodeRefsByEpisodeId(String episodeId) {
        try {
            String seriesId = getSeriesIdByEpisodeId(episodeId);
            List<String> tags = tagRepository.findTagsBySeriesId(seriesId);
            List<String> categories = categoryRepository.findCategoriesBySeriesId(seriesId);
            return new EpisodeRefs(episodeId, tags, categories);

        } catch (IllegalArgumentException e) {
            return new EpisodeRefs(episodeId, Collections.emptyList(), Collections.emptyList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EpisodeLogResponseDto> getEpisodeLogs(String episodeId, LocalDateTime from, LocalDateTime to, String accountId) {
        List<EpisodeLog> logs = episodeLogRepository.findByEpisode_EpisodeIdAndHourBucketBetweenOrderByHourBucketAsc(
                episodeId, from, to
        );
        return logs.stream()
                .map(log -> EpisodeLogResponseDto.builder()
                        .episodeLogId(log.getEpisodeLogId())
                        .hourBucket(log.getHourBucket())
                        .analyticData(log.getAnalyticData())
                        .build())
                .toList();
    }

    @Override
    public EpisodeResponseDto toResponse(Episode episode) {
        return EpisodeResponseDto.builder()
                .episodeId(episode.getEpisodeId())
                .seasonId(episode.getSeason().getSeasonId())
                .seriesId(episode.getSeason().getSeries().getSeriesId())
                .seriesTitle(episode.getSeason().getSeries().getTitle())
                .creatorId(episode.getCreatorId())
                .episodeNumber(episode.getEpisodeNumber())
                .title(episode.getTitle())
                .description(episode.getDescription())
                .thumbnail(episode.getThumbnail())
                .contentType(episode.getContentType())
                .status(episode.getStatus())
                .scheduledPublishAt(episode.getScheduledPublishAt())
                .publishedAt(episode.getPublishedAt())
                .unlockType(episode.getUnlockType())
                .priceVnd(episode.getPriceVnd())
                .analyticData(episode.getAnalyticData())
                .totalPage(episode.getTotalPage())
                .createdAt(episode.getCreatedAt())
                .updatedAt(episode.getUpdatedAt())
                .deletedAt(episode.getDeletedAt())
                .isDeleted(episode.getIsDeleted())
                .build();
    }

    private void validateEpisodeContentType(Season season, ContentType contentType) {
        if (season.getSeries().getContentType() != contentType) {
            throw ContentModuleException.badRequest("Episode content type must match series content type");
        }
    }

    private void ensureReadyMediaForPublish(Episode episode) {
        long totalMedia = mediaRepository.countByEpisode_EpisodeIdAndIsDeletedFalse(episode.getEpisodeId());
        if (totalMedia == 0) {
            throw ContentModuleException.badRequest("Episode must have at least one media before publishing");
        }

        boolean hasUnapprovedMedia = mediaRepository.existsByEpisode_EpisodeIdAndApprovalStatusNotAndIsDeletedFalse(
                episode.getEpisodeId(),
                ContentApprovalStatus.APPROVED);

        if (hasUnapprovedMedia) {
            throw ContentModuleException.badRequest("All media in the episode must have approval_status as APPROVED (pending_review or rejected are not allowed)");
        }

        long readyMedia = mediaRepository.countByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                episode.getEpisodeId(),
                requiredMediaType(episode),
                readyMediaStatuses(episode));

        if (totalMedia != readyMedia) {
            throw ContentModuleException.badRequest("All media must be processed and ready (e.g., ACTIVE or HLS_READY) before publishing");
        }
    }

    private MediaType requiredMediaType(Episode episode) {
        return episode.getContentType() == ContentType.VIDEO ? MediaType.VIDEO : MediaType.IMAGE;
    }

    private List<MediaStatus> readyMediaStatuses(Episode episode) {
        return episode.getContentType() == ContentType.VIDEO
                ? List.of(MediaStatus.HLS_READY, MediaStatus.ACTIVE)
                : List.of(MediaStatus.ACTIVE);
    }

    private void ensureEpisodeCanBeScheduled(Episode episode) {
        if (episode.getStatus() == EpisodeStatus.PUBLISHED) {
            throw ContentModuleException.badRequest("A published episode cannot be scheduled again; hide it first");
        }
        if (episode.getStatus() == EpisodeStatus.DELETED) {
            throw ContentModuleException.badRequest("A deleted episode cannot be scheduled");
        }
        // Cùng lý do chặn ở publish(): chỉ Admin (forceUnhide) mới được gỡ FORCE_HIDDEN.
        if (episode.getStatus() == EpisodeStatus.FORCE_HIDDEN) {
            throw ContentModuleException.badRequest("Episode is force-hidden by admin and cannot be scheduled by the creator");
        }
        ensureParentsAreNotDeleted(episode);
    }

    private void prepareParentsForSchedule(Episode episode, String actorId) {
        Season season = episode.getSeason();
        Series series = season.getSeries();
        ensureParentsAreNotDeleted(episode);

        if (season.getStatus() == SeasonStatus.DRAFT) {
            season.setStatus(SeasonStatus.SCHEDULED);
            season.setReleasedUpdateTime(LocalDateTime.now());
        }
        if (series.getStatus() == SeriesStatus.DRAFT && season.getStatus() == SeasonStatus.SCHEDULED) {
            series.setStatus(SeriesStatus.SCHEDULED);
            series.setReleasedUpdateTime(LocalDateTime.now());
        }
    }

    private void publishScheduledParents(Episode episode, String actorId) {
        Season season = episode.getSeason();
        Series series = season.getSeries();
        ensureParentsAreNotDeleted(episode);

        if (season.getStatus() == SeasonStatus.SCHEDULED) {
            season.setStatus(SeasonStatus.PUBLISHED);
            season.setReleasedUpdateTime(LocalDateTime.now());
        }
        if (series.getStatus() == SeriesStatus.SCHEDULED && season.getStatus() == SeasonStatus.PUBLISHED) {
            series.setStatus(SeriesStatus.PUBLISHED);
            series.setReleasedUpdateTime(LocalDateTime.now());
        }
    }

    private void publishParentsImmediately(Episode episode, String actorId) {
        Season season = episode.getSeason();
        Series series = season.getSeries();
        ensureParentsAreNotDeleted(episode);

        if (season.getStatus() != SeasonStatus.PUBLISHED) {
            season.setStatus(SeasonStatus.PUBLISHED);
            season.setReleasedUpdateTime(LocalDateTime.now());
        }
        if (series.getStatus() != SeriesStatus.PUBLISHED) {
            series.setStatus(SeriesStatus.PUBLISHED);
            series.setReleasedUpdateTime(LocalDateTime.now());
        }
    }

    private void ensureParentsAreNotDeleted(Episode episode) {
        if (episode.getSeason().getStatus() == SeasonStatus.DELETED
                || episode.getSeason().getSeries().getStatus() == SeriesStatus.DELETED) {
            throw ContentModuleException.badRequest("Cannot publish an episode whose season or series is deleted");
        }
    }

    private void cancelScheduledPublication(Episode episode, String actorId) {
        episode.setScheduledPublishAt(null);
        restoreSeasonAfterScheduleCancellation(episode, actorId);
        restoreSeriesAfterScheduleCancellation(episode, actorId);
    }

    private void restoreSeasonAfterScheduleCancellation(Episode episode, String actorId) {
        Season season = episode.getSeason();
        if (season.getStatus() != SeasonStatus.SCHEDULED) {
            return;
        }

        long publishedEpisodes = episodeRepository.countBySeasonIdExcludingEpisodeAndStatus(
                season.getSeasonId(), episode.getEpisodeId(), EpisodeStatus.PUBLISHED);
        long scheduledEpisodes = episodeRepository.countBySeasonIdExcludingEpisodeAndStatus(
                season.getSeasonId(), episode.getEpisodeId(), EpisodeStatus.SCHEDULED);
        if (publishedEpisodes > 0) {
            season.setStatus(SeasonStatus.PUBLISHED);
            season.setReleasedUpdateTime(LocalDateTime.now());
        } else if (scheduledEpisodes == 0) {
            season.setStatus(SeasonStatus.DRAFT);
            season.setReleasedUpdateTime(LocalDateTime.now());
        }
    }

    private void restoreSeriesAfterScheduleCancellation(Episode episode, String actorId) {
        Series series = episode.getSeason().getSeries();
        if (series.getStatus() != SeriesStatus.SCHEDULED) {
            return;
        }

        long publishedEpisodes = episodeRepository.countBySeriesIdExcludingEpisodeAndStatus(
                series.getSeriesId(), episode.getEpisodeId(), EpisodeStatus.PUBLISHED);
        long scheduledEpisodes = episodeRepository.countBySeriesIdExcludingEpisodeAndStatus(
                series.getSeriesId(), episode.getEpisodeId(), EpisodeStatus.SCHEDULED);
        if (publishedEpisodes > 0) {
            series.setStatus(SeriesStatus.PUBLISHED);
            series.setReleasedUpdateTime(LocalDateTime.now());
        } else if (scheduledEpisodes == 0) {
            series.setStatus(SeriesStatus.DRAFT);
            series.setReleasedUpdateTime(LocalDateTime.now());
        }
    }

    private void applyFreeUnlockSettings(Episode episode) {
        episode.setUnlockType(EpisodeUnlockType.FREE);
        episode.setPriceVnd(0L);
    }

    private void applyUnlockSettings(Episode episode, EpisodeUnlockSettingsRequestDto request) {
        EpisodeUnlockType unlockType = request.getUnlockType();
        Long priceVnd = request.getPriceVnd();

        if (unlockType == EpisodeUnlockType.FREE) {
            applyFreeUnlockSettings(episode);
            return;
        }

        if (priceVnd == null || priceVnd <= 0 || priceVnd >= 100_000L) {
            throw ContentModuleException.badRequest("Paid episode price must be greater than 0 and less than 100,000 VND");
        }
        episode.setUnlockType(EpisodeUnlockType.PAID);
        episode.setPriceVnd(priceVnd);
    }

    private void ensureAccountHasCreatorRole(String accountId) {
        Account account = accountRepository.findById(UUID.fromString(accountId))
                .orElseThrow(() -> ContentModuleException.forbidden("Only creator accounts can update episode price settings"));

        if (account.getRole() == null
                || !Long.valueOf(CREATOR_ROLE_ID).equals(account.getRole().getRoleId())) {
            throw ContentModuleException.forbidden("Only creator accounts can update episode price settings");
        }
    }

    private void ensureScheduledPublishAt(LocalDateTime scheduledPublishAt) {
        if (scheduledPublishAt == null) {
            throw ContentModuleException.badRequest("scheduledPublishAt is required");
        }
        if (!scheduledPublishAt.isAfter(LocalDateTime.now())) {
            throw ContentModuleException.badRequest("scheduledPublishAt must be in the future");
        }
    }

    private void ensureScheduledEpisodeIsDue(Episode episode) {
        if (episode.getStatus() != EpisodeStatus.SCHEDULED || episode.getScheduledPublishAt() == null) {
            throw ContentModuleException.badRequest("Episode is not scheduled for publishing");
        }
        if (episode.getScheduledPublishAt().isAfter(LocalDateTime.now())) {
            throw ContentModuleException.badRequest("Episode is not due for publishing yet");
        }
    }

    private int nextEpisodeNumber(String seasonId) {
        return episodeRepository.findMaxEpisodeNumberBySeasonId(seasonId) + 1;
    }
}
