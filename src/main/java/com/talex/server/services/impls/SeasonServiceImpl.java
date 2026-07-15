package com.talex.server.services.impls;

import com.talex.server.dtos.requests.SeasonRequestDto;
import com.talex.server.dtos.responses.SeasonResponseDto;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.series.SeasonStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.series.SeasonRepository;
import com.talex.server.services.ContentOwnershipService;
import com.talex.server.services.SeasonService;
import com.talex.server.services.SeriesService;
import com.talex.server.services.audit.ContentAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonServiceImpl implements SeasonService {
    private final SeasonRepository seasonRepository;
    private final SeriesService seriesService;
    private final ContentOwnershipService contentOwnershipService;
    private final ContentAuditLogger contentAuditLogger;

    @Transactional
    @Override
    public SeasonResponseDto create(String seriesId, SeasonRequestDto request, String accountId) {
        Series series = seriesService.findActiveEntity(seriesId);
        contentOwnershipService.assertCanManage(series, accountId);

        Season season = new Season();
        season.setSeries(series);
        season.setCreatorId(series.getCreator().getCreatorId());
        season.setSeasonNumber(request.getSeasonNumber() != null
                ? request.getSeasonNumber()
                : nextSeasonNumber(seriesId));
        season.setTitle(request.getTitle());
        season.setDescription(request.getDescription());
        season.setStatus(SeasonStatus.DRAFT);

        Season saved = seasonRepository.save(season);
        contentAuditLogger.logAction("Season", saved.getSeasonId(), "CREATE", accountId, saved.getCreatorId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public SeasonResponseDto getById(String id, String accountId) {
        Season season = findActiveEntity(id);
        contentOwnershipService.assertCanView(season, accountId);
        return toResponse(season);
    }

    @Transactional(readOnly = true)
    @Override
    public SeasonResponseDto getPublicById(String id) {
        return toResponse(findPublicEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<SeasonResponseDto> listBySeries(String seriesId, String accountId) {
        Series series = seriesService.findActiveEntity(seriesId);
        contentOwnershipService.assertCanView(series, accountId);
        return seasonRepository.findAllBySeries_SeriesIdAndIsDeletedFalseOrderBySeasonNumberAsc(seriesId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<SeasonResponseDto> listPublicBySeries(String seriesId) {
        seriesService.findPublicEntity(seriesId);
        return seasonRepository
                .findAllBySeries_SeriesIdAndStatusInAndIsDeletedFalseOrderBySeasonNumberAsc(
                        seriesId,
                        List.of(SeasonStatus.PUBLISHED, SeasonStatus.SCHEDULED))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public SeasonResponseDto update(String id, SeasonRequestDto request, String accountId) {
        Season season = findManageableEntity(id, accountId);
        if (request.getStatus() == SeasonStatus.SCHEDULED) {
            throw ContentModuleException.badRequest("SCHEDULED is managed by episode publish scheduling");
        }
        if (request.getSeasonNumber() != null) {
            season.setSeasonNumber(request.getSeasonNumber());
        }
        season.setTitle(request.getTitle());
        season.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            season.setStatus(request.getStatus());
        }

        Season saved = seasonRepository.save(season);
        contentAuditLogger.logAction("Season", saved.getSeasonId(), "UPDATE", accountId, saved.getCreatorId());
        return toResponse(saved);
    }

    @Transactional
    @Override
    public SeasonResponseDto hide(String id, String actorId) {
        Season season = findManageableEntity(id, actorId);
        season.setStatus(SeasonStatus.HIDDEN);
        Season saved = seasonRepository.save(season);
        contentAuditLogger.logAction("Season", saved.getSeasonId(), "HIDE", actorId, saved.getCreatorId());
        return toResponse(saved);
    }

    @Transactional
    @Override
    public SeasonResponseDto unhide(String id, String actorId) {
        Season season = findActiveEntity(id);
        contentOwnershipService.assertCanManage(season, actorId);
        season.setStatus(SeasonStatus.PUBLISHED);
        Season saved = seasonRepository.save(season);
        contentAuditLogger.logAction("Season", saved.getSeasonId(), "UNHIDE", actorId, season.getSeries().getCreator().getCreatorId());
        return toResponse(saved);
    }

    @Transactional
    @Override
    public SeasonResponseDto forceHide(String id, String actorId) {
        Season season = findActiveEntity(id);
        season.setStatus(SeasonStatus.FORCE_HIDDEN);
        Season saved = seasonRepository.save(season);
        contentAuditLogger.logAction("Season", saved.getSeasonId(), "FORCE_HIDE", actorId, season.getSeries().getCreator().getCreatorId());
        return toResponse(saved);
    }

    @Transactional
    @Override
    public SeasonResponseDto forceUnhide(String id, String actorId) {
        Season season = findActiveEntity(id);
        if (season.getStatus() != SeasonStatus.FORCE_HIDDEN) {
            throw ContentModuleException.badRequest("Season is not force-hidden");
        }
        season.setStatus(SeasonStatus.HIDDEN);
        Season saved = seasonRepository.save(season);
        contentAuditLogger.logAction("Season", saved.getSeasonId(), "FORCE_UNHIDE", actorId, season.getSeries().getCreator().getCreatorId());
        return toResponse(saved);
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Season season = findManageableEntity(id, actorId);
        season.setStatus(SeasonStatus.DELETED);
        season.softDelete();
        seasonRepository.save(season);
        contentAuditLogger.logAction("Season", season.getSeasonId(), "DELETE", actorId, season.getCreatorId());
    }

    @Override
    public Season findActiveEntity(String id) {
        return seasonRepository.findBySeasonIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Season not found: " + id));
    }

    private Season findManageableEntity(String id, String accountId) {
        Season season = findActiveEntity(id);
        contentOwnershipService.assertCanManage(season, accountId);
        return season;
    }

    @Override
    public Season findPublicEntity(String id) {
        Season season = findActiveEntity(id);
        if (season.getStatus() != SeasonStatus.PUBLISHED && season.getStatus() != SeasonStatus.SCHEDULED) {
            throw ContentModuleException.notFound("Public season not found: " + id);
        }
        Series series = season.getSeries();
        if (series.getStatus() != SeriesStatus.PUBLISHED && series.getStatus() != SeriesStatus.SCHEDULED) {
            throw ContentModuleException.notFound("Public series not found: " + series.getSeriesId());
        }
        return season;
    }

    @Override
    public SeasonResponseDto toResponse(Season season) {
        return SeasonResponseDto.builder()
                .seasonId(season.getSeasonId())
                .seriesId(season.getSeries().getSeriesId())
                .creatorId(season.getCreatorId())
                .seasonNumber(season.getSeasonNumber())
                .title(season.getTitle())
                .description(season.getDescription())
                .status(season.getStatus())
                .createdAt(season.getCreatedAt())
                .updatedAt(season.getUpdatedAt())
                .deletedAt(season.getDeletedAt())
                .isDeleted(season.getIsDeleted())
                .build();
    }

    private int nextSeasonNumber(String seriesId) {
        return seasonRepository.findMaxSeasonNumberBySeriesId(seriesId) + 1;
    }

}
