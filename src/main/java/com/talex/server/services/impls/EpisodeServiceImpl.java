package com.talex.server.services.impls;

import com.talex.server.dtos.requests.EpisodeRequestDto;
import com.talex.server.dtos.responses.EpisodeResponseDto;
import com.talex.server.entities.Episode;
import com.talex.server.entities.Media;
import com.talex.server.entities.Season;
import com.talex.server.entities.Series;
import com.talex.server.enums.ContentApprovalStatus;
import com.talex.server.enums.ContentType;
import com.talex.server.enums.EpisodeStatus;
import com.talex.server.enums.EpisodeUnlockType;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import com.talex.server.enums.SeasonStatus;
import com.talex.server.enums.SeriesStatus;
import com.talex.server.enums.Visibility;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.EpisodeRepository;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.services.EpisodeService;
import com.talex.server.services.SeasonService;
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

    @Transactional
    @Override
    public EpisodeResponseDto create(String seasonId, EpisodeRequestDto request) {
        Season season = seasonService.findActiveEntity(seasonId);
        ContentType contentType = request.getContentType() != null
                ? request.getContentType()
                : season.getSeries().getContentType();
        validateEpisodeContentType(season, contentType);

        Episode episode = new Episode();
        episode.setSeason(season);
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
        episode.markCreatedBy(request.getActorId());

        return toResponse(episodeRepository.save(episode));
    }

    @Transactional(readOnly = true)
    @Override
    public EpisodeResponseDto getById(String id) {
        return toResponse(findActiveEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public EpisodeResponseDto getPublicById(String id) {
        return toResponse(findPublicEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<EpisodeResponseDto> listBySeason(String seasonId) {
        seasonService.findActiveEntity(seasonId);
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
    public EpisodeResponseDto update(String id, EpisodeRequestDto request) {
        Episode episode = findActiveEntity(id);
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
            if (request.getStatus() == EpisodeStatus.PUBLISHED) {
                ensureReadyMediaForPublish(episode);
                if (episode.getPublishedAt() == null) {
                    episode.setPublishedAt(LocalDateTime.now());
                }
            }
            episode.setStatus(request.getStatus());
        }
        episode.setTotalPage(request.getTotalPage());
        applyUnlockSettings(episode, request);
        episode.markUpdatedBy(request.getActorId());

        return toResponse(episodeRepository.save(episode));
    }

    @Transactional
    @Override
    public EpisodeResponseDto schedulePublish(String id, LocalDateTime scheduledPublishAt, String actorId) {
        Episode episode = findActiveEntity(id);
        ensureScheduledPublishAt(scheduledPublishAt);
        List<Media> mediaToHide = findReadyMediaForPublish(episode);
        if (mediaToHide.isEmpty()) {
            throw ContentModuleException.badRequest("Episode must have at least one ready media before scheduling");
        }
        boolean shouldPublishParents = shouldPublishParentsWithScheduledEpisode(episode);
        if (shouldPublishParents) {
            hideParentContentForSchedule(episode, actorId);
        }
        hideEpisodeMediaForSchedule(mediaToHide, actorId);
        episode.setStatus(EpisodeStatus.HIDDEN);
        episode.setScheduledPublishAt(scheduledPublishAt);
        episode.markUpdatedBy(actorId);
        mediaRepository.saveAll(mediaToHide);
        return toResponse(episodeRepository.save(episode));
    }

    @Transactional
    @Override
    public EpisodeResponseDto publish(String id, String actorId) {
        Episode episode = findActiveEntity(id);
        ensureReadyMediaForPublish(episode);

        episode.setStatus(EpisodeStatus.PUBLISHED);
        episode.setScheduledPublishAt(null);
        if (episode.getPublishedAt() == null) {
            episode.setPublishedAt(LocalDateTime.now());
        }
        episode.markUpdatedBy(actorId);
        return toResponse(episodeRepository.save(episode));
    }

    @Transactional
    @Override
    public EpisodeResponseDto publishScheduled(String id, String actorId) {
        Episode episode = findActiveEntity(id);
        List<Media> mediaToPublish = findScheduledMediaForPublish(episode);
        if (mediaToPublish.isEmpty()) {
            throw ContentModuleException.badRequest("Episode must have at least one scheduled or ready media before publishing");
        }

        boolean shouldPublishParents = shouldPublishParentsWithScheduledEpisode(episode);
        if (shouldPublishParents) {
            publishParentContentForSchedule(episode, actorId);
        }

        for (Media media : mediaToPublish) {
            if (media.getStatus() == MediaStatus.HIDDEN) {
                media.setStatus(MediaStatus.ACTIVE);
            }
            media.markUpdatedBy(actorId);
        }
        episode.setStatus(EpisodeStatus.PUBLISHED);
        episode.setScheduledPublishAt(null);
        if (episode.getPublishedAt() == null) {
            episode.setPublishedAt(LocalDateTime.now());
        }
        episode.markUpdatedBy(actorId);
        mediaRepository.saveAll(mediaToPublish);
        return toResponse(episodeRepository.save(episode));
    }

    @Transactional
    @Override
    public EpisodeResponseDto hide(String id, String actorId) {
        Episode episode = findActiveEntity(id);
        episode.setStatus(EpisodeStatus.HIDDEN);
        episode.markUpdatedBy(actorId);
        return toResponse(episodeRepository.save(episode));
    }

    @Transactional
    @Override
    public EpisodeResponseDto unhide(String id, String actorId) {
        Episode episode = findActiveEntity(id);
        ensureReadyMediaForPublish(episode);
        episode.setStatus(EpisodeStatus.PUBLISHED);
        if (episode.getPublishedAt() == null) {
            episode.setPublishedAt(LocalDateTime.now());
        }
        episode.markUpdatedBy(actorId);
        return toResponse(episodeRepository.save(episode));
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Episode episode = findActiveEntity(id);
        episode.setStatus(EpisodeStatus.DELETED);
        episode.softDelete(actorId);
        episodeRepository.save(episode);
    }

    @Override
    public Episode findActiveEntity(String id) {
        return episodeRepository.findByEpisodeIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Episode not found: " + id));
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
                .createdBy(episode.getCreatedBy())
                .updatedBy(episode.getUpdatedBy())
                .deletedBy(episode.getDeletedBy())
                .isDeleted(episode.getIsDeleted())
                .build();
    }

    private void validateEpisodeContentType(Season season, ContentType contentType) {
        if (season.getSeries().getContentType() != contentType) {
            throw ContentModuleException.badRequest("Episode content type must match series content type");
        }
    }

    private void ensureReadyMediaForPublish(Episode episode) {
        boolean hasReadyMedia = mediaRepository.existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndApprovalStatusAndIsDeletedFalse(
                episode.getEpisodeId(),
                requiredMediaType(episode),
                readyMediaStatuses(episode),
                ContentApprovalStatus.APPROVED);
        if (!hasReadyMedia) {
            throw ContentModuleException.badRequest("Episode must have at least one approved ready media before publishing");
        }
    }

    private List<Media> findReadyMediaForPublish(Episode episode) {
        return mediaRepository.findAllByEpisode_EpisodeIdAndMediaTypeAndStatusInAndApprovalStatusAndIsDeletedFalse(
                episode.getEpisodeId(),
                requiredMediaType(episode),
                readyMediaStatuses(episode),
                ContentApprovalStatus.APPROVED);
    }

    private List<Media> findScheduledMediaForPublish(Episode episode) {
        return mediaRepository.findAllByEpisode_EpisodeIdAndMediaTypeAndStatusInAndApprovalStatusAndIsDeletedFalse(
                episode.getEpisodeId(),
                requiredMediaType(episode),
                scheduledPublishMediaStatuses(episode),
                ContentApprovalStatus.APPROVED);
    }

    private MediaType requiredMediaType(Episode episode) {
        return episode.getContentType() == ContentType.VIDEO ? MediaType.VIDEO : MediaType.IMAGE;
    }

    private List<MediaStatus> readyMediaStatuses(Episode episode) {
        return episode.getContentType() == ContentType.VIDEO
                ? List.of(MediaStatus.HLS_READY, MediaStatus.ACTIVE)
                : List.of(MediaStatus.ACTIVE);
    }

    private List<MediaStatus> scheduledPublishMediaStatuses(Episode episode) {
        List<MediaStatus> statuses = new java.util.ArrayList<>(readyMediaStatuses(episode));
        statuses.add(MediaStatus.HIDDEN);
        return statuses;
    }

    private boolean shouldPublishParentsWithScheduledEpisode(Episode episode) {
        String seriesId = episode.getSeason().getSeries().getSeriesId();
        long publishedEpisodes = episodeRepository.countBySeriesIdExcludingEpisodeAndStatus(
                seriesId,
                episode.getEpisodeId(),
                EpisodeStatus.PUBLISHED);
        return publishedEpisodes == 0;
    }

    private void hideParentContentForSchedule(Episode episode, String actorId) {
        Season season = episode.getSeason();
        Series series = season.getSeries();
        if (series.getStatus() != SeriesStatus.DELETED) {
            series.setStatus(SeriesStatus.HIDDEN);
            series.markUpdatedBy(actorId);
        }
        if (season.getStatus() != SeasonStatus.DELETED) {
            season.setStatus(SeasonStatus.HIDDEN);
            season.markUpdatedBy(actorId);
        }
    }

    private void publishParentContentForSchedule(Episode episode, String actorId) {
        Season season = episode.getSeason();
        Series series = season.getSeries();
        series.setStatus(SeriesStatus.PUBLISHED);
        series.setVisibility(Visibility.PUBLIC);
        series.markUpdatedBy(actorId);
        season.setStatus(SeasonStatus.PUBLISHED);
        season.markUpdatedBy(actorId);
    }

    private void hideEpisodeMediaForSchedule(List<Media> mediaToHide, String actorId) {
        for (Media media : mediaToHide) {
            media.setStatus(MediaStatus.HIDDEN);
            media.markUpdatedBy(actorId);
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
    }

    private int nextEpisodeNumber(String seasonId) {
        return episodeRepository.findAllBySeason_SeasonIdAndIsDeletedFalseOrderByEpisodeNumberAsc(seasonId)
                .stream()
                .map(Episode::getEpisodeNumber)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }
}
