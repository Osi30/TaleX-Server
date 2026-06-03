package com.talex.server.services.impls;

import com.talex.server.dtos.requests.EpisodeRequestDto;
import com.talex.server.dtos.responses.EpisodeResponseDto;
import com.talex.server.entities.Episode;
import com.talex.server.entities.Season;
import com.talex.server.enums.ContentType;
import com.talex.server.enums.EpisodeStatus;
import com.talex.server.enums.MediaStatus;
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
        episode.setStatus(request.getStatus() != null ? request.getStatus() : EpisodeStatus.DRAFT);
        episode.setTotalPage(request.getTotalPage());
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
            episode.setStatus(request.getStatus());
        }
        episode.setTotalPage(request.getTotalPage());
        episode.markUpdatedBy(request.getActorId());

        return toResponse(episodeRepository.save(episode));
    }

    @Transactional
    @Override
    public EpisodeResponseDto publish(String id, String actorId) {
        Episode episode = findActiveEntity(id);
        boolean hasActiveMedia = !mediaRepository
                .findAllByEpisode_EpisodeIdAndStatusAndIsDeletedFalseOrderByDisplayOrderAsc(
                        id,
                        MediaStatus.ACTIVE)
                .isEmpty();
        if (!hasActiveMedia) {
            throw ContentModuleException.badRequest("Episode must have at least one active media before publishing");
        }

        episode.setStatus(EpisodeStatus.PUBLISHED);
        if (episode.getPublishedAt() == null) {
            episode.setPublishedAt(LocalDateTime.now());
        }
        episode.markUpdatedBy(actorId);
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
        episode.setStatus(EpisodeStatus.PUBLISHED);
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
                .publishedAt(episode.getPublishedAt())
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

    private int nextEpisodeNumber(String seasonId) {
        return episodeRepository.findAllBySeason_SeasonIdAndIsDeletedFalseOrderByEpisodeNumberAsc(seasonId)
                .stream()
                .map(Episode::getEpisodeNumber)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }
}
