package com.talex.server.services.impls;

import com.talex.server.dtos.requests.EpisodeRequestDto;
import com.talex.server.dtos.responses.EpisodeResponseDto;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.enums.series.EpisodeUnlockType;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.enums.series.SeasonStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.services.ContentOwnershipService;
import com.talex.server.services.EpisodeService;
import com.talex.server.services.SeasonService;
import com.talex.server.services.audit.ContentAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EpisodeServiceImpl implements EpisodeService {
    private final EpisodeRepository episodeRepository;
    private final MediaRepository mediaRepository;
    private final SeasonService seasonService;
    private final ContentOwnershipService contentOwnershipService;
    private final ContentAuditLogger contentAuditLogger;

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
        episode.setContentType(contentType);
        episode.setStatus(EpisodeStatus.DRAFT);
        episode.setScheduledPublishAt(null);
        episode.setTotalPage(request.getTotalPage());
        applyUnlockSettings(episode, request);
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "CREATE", accountId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional(readOnly = true)
    @Override
    public EpisodeResponseDto getById(String id, String accountId) {
        Episode episode = findManageableEntity(id, accountId);
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
        contentOwnershipService.assertCanManage(season.getSeries(), accountId);
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
                .findAllBySeason_SeasonIdAndStatusAndIsDeletedFalseOrderByEpisodeNumberAsc(
                        seasonId,
                        EpisodeStatus.PUBLISHED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public EpisodeResponseDto update(String id, EpisodeRequestDto request, String accountId) {
        Episode episode = findManageableEntity(id, accountId);
        if (request.getEpisodeNumber() != null) {
            episode.setEpisodeNumber(request.getEpisodeNumber());
        }
        episode.setTitle(request.getTitle());
        episode.setDescription(request.getDescription());
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
            episode.setStatus(request.getStatus());
        }
        episode.setTotalPage(request.getTotalPage());
        applyUnlockSettings(episode, request);
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "UPDATE", accountId, episode.getCreatorId());
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
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "CANCEL_SCHEDULE", actorId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional
    @Override
    public EpisodeResponseDto publish(String id, String actorId) {
        Episode episode = findManageableEntity(id, actorId);
        ensureReadyMediaForPublish(episode);
        publishParentsImmediately(episode, actorId);

        episode.setStatus(EpisodeStatus.PUBLISHED);
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
        episodeRepository.save(episode);
        contentAuditLogger.logAction("Episode", episode.getEpisodeId(), "HIDE", actorId, episode.getCreatorId());
        return toResponse(episode);
    }

    @Transactional
    @Override
    public EpisodeResponseDto unhide(String id, String actorId) {
        Episode episode = findManageableEntity(id, actorId);
        ensureReadyMediaForPublish(episode);
        publishParentsImmediately(episode, actorId);
        episode.setStatus(EpisodeStatus.PUBLISHED);
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
        episode.softDelete();
        episodeRepository.save(episode);
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
        if (contentOwnershipService.isPrivileged()) {
            return findActiveEntity(id);
        }

        String creatorId = contentOwnershipService.requireCurrentCreatorId(accountId);
        Episode episode = episodeRepository
                .findByEpisodeIdAndCreatorIdAndIsDeletedFalse(id, creatorId)
                .orElseThrow(() -> ContentModuleException.notFound("Episode not found: " + id));
        contentOwnershipService.assertOwnedByCreator(episode, creatorId);
        return episode;
    }

    @Override
    public Episode findPublicEntity(String id) {
        Episode episode = findActiveEntity(id);
        if (episode.getStatus() != EpisodeStatus.PUBLISHED) {
            throw ContentModuleException.notFound("Public episode not found: " + id);
        }
        seasonService.findPublicEntity(episode.getSeason().getSeasonId());
        return episode;
    }

    @Override
    public EpisodeResponseDto toResponse(Episode episode) {
        return EpisodeResponseDto.builder()
                .episodeId(episode.getEpisodeId())
                .seasonId(episode.getSeason().getSeasonId())
                .creatorId(episode.getCreatorId())
                .episodeNumber(episode.getEpisodeNumber())
                .title(episode.getTitle())
                .description(episode.getDescription())
                .contentType(episode.getContentType())
                .status(episode.getStatus())
                .scheduledPublishAt(episode.getScheduledPublishAt())
                .publishedAt(episode.getPublishedAt())
                .unlockType(episode.getUnlockType())
                .priceVnd(episode.getPriceVnd())
                .likes(episode.getLikes())
                .views(episode.getViews())
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
        ensureParentsAreNotDeleted(episode);
    }

    private void prepareParentsForSchedule(Episode episode, String actorId) {
        Season season = episode.getSeason();
        Series series = season.getSeries();
        ensureParentsAreNotDeleted(episode);

        if (season.getStatus() == SeasonStatus.DRAFT) {
            season.setStatus(SeasonStatus.SCHEDULED);
        }
        if (series.getStatus() == SeriesStatus.DRAFT && season.getStatus() != SeasonStatus.HIDDEN) {
            series.setStatus(SeriesStatus.SCHEDULED);
        }
    }

    private void publishScheduledParents(Episode episode, String actorId) {
        Season season = episode.getSeason();
        Series series = season.getSeries();
        ensureParentsAreNotDeleted(episode);

        if (season.getStatus() == SeasonStatus.SCHEDULED) {
            season.setStatus(SeasonStatus.PUBLISHED);
        }
        if (series.getStatus() == SeriesStatus.SCHEDULED && season.getStatus() == SeasonStatus.PUBLISHED) {
            series.setStatus(SeriesStatus.PUBLISHED);
        }
    }

    private void publishParentsImmediately(Episode episode, String actorId) {
        Season season = episode.getSeason();
        Series series = season.getSeries();
        ensureParentsAreNotDeleted(episode);

        if (season.getStatus() != SeasonStatus.PUBLISHED) {
            season.setStatus(SeasonStatus.PUBLISHED);
        }
        if (series.getStatus() != SeriesStatus.PUBLISHED) {
            series.setStatus(SeriesStatus.PUBLISHED);
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
        } else if (scheduledEpisodes == 0) {
            season.setStatus(SeasonStatus.DRAFT);
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
        } else if (scheduledEpisodes == 0) {
            series.setStatus(SeriesStatus.DRAFT);
        }
    }

    private void applyUnlockSettings(Episode episode, EpisodeRequestDto request) {
        EpisodeUnlockType unlockType = request.getUnlockType() != null
                ? request.getUnlockType()
                : episode.getUnlockType();
        Long priceVnd = request.getPriceVnd() != null
                ? request.getPriceVnd()
                : episode.getPriceVnd();

        if (unlockType == EpisodeUnlockType.FREE) {
            episode.setUnlockType(EpisodeUnlockType.FREE);
            episode.setPriceVnd(0L);
            return;
        }

        if (priceVnd == null || priceVnd <= 0 || priceVnd >= 100_000L) {
            throw ContentModuleException.badRequest("Paid episode price must be greater than 0 and less than 100,000 VND");
        }
        episode.setUnlockType(EpisodeUnlockType.PAID);
        episode.setPriceVnd(priceVnd);
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
