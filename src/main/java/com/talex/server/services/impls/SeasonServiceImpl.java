package com.talex.server.services.impls;

import com.talex.server.dtos.requests.SeasonRequestDto;
import com.talex.server.dtos.responses.SeasonResponseDto;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.SeasonStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.series.SeasonRepository;
import com.talex.server.services.ContentOwnershipService;
import com.talex.server.services.SeasonService;
import com.talex.server.services.SeriesService;
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
        season.markCreatedBy(accountId);

        return toResponse(seasonRepository.save(season));
    }

    @Transactional(readOnly = true)
    @Override
    public SeasonResponseDto getById(String id, String accountId) {
        Season season = findManageableEntity(id, accountId);
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
        contentOwnershipService.assertCanManage(series, accountId);
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
                .findAllBySeries_SeriesIdAndStatusAndIsDeletedFalseOrderBySeasonNumberAsc(
                        seriesId,
                        SeasonStatus.PUBLISHED)
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
        season.markUpdatedBy(accountId);

        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public SeasonResponseDto publish(String id, String actorId) {
        Season season = findManageableEntity(id, actorId);
        season.setStatus(SeasonStatus.PUBLISHED);
        season.markUpdatedBy(actorId);
        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public SeasonResponseDto hide(String id, String actorId) {
        Season season = findManageableEntity(id, actorId);
        season.setStatus(SeasonStatus.HIDDEN);
        season.markUpdatedBy(actorId);
        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public SeasonResponseDto unhide(String id, String actorId) {
        Season season = findManageableEntity(id, actorId);
        season.setStatus(SeasonStatus.PUBLISHED);
        season.markUpdatedBy(actorId);
        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Season season = findManageableEntity(id, actorId);
        season.setStatus(SeasonStatus.DELETED);
        season.softDelete(actorId);
        seasonRepository.save(season);
    }

    @Override
    public Season findActiveEntity(String id) {
        return seasonRepository.findBySeasonIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Season not found: " + id));
    }

    private Season findManageableEntity(String id, String accountId) {
        if (contentOwnershipService.isPrivileged()) {
            return findActiveEntity(id);
        }

        String creatorId = contentOwnershipService.requireCurrentCreatorId(accountId);
        Season season = seasonRepository
                .findBySeasonIdAndCreatorIdAndIsDeletedFalse(id, creatorId)
                .orElseThrow(() -> ContentModuleException.notFound("Season not found: " + id));
        contentOwnershipService.assertOwnedByCreator(season, creatorId);
        return season;
    }

    @Override
    public Season findPublicEntity(String id) {
        Season season = findActiveEntity(id);
        if (season.getStatus() != SeasonStatus.PUBLISHED) {
            throw ContentModuleException.notFound("Public season not found: " + id);
        }
        seriesService.findPublicEntity(season.getSeries().getSeriesId());
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
                .createdBy(season.getCreatedBy())
                .updatedBy(season.getUpdatedBy())
                .deletedBy(season.getDeletedBy())
                .isDeleted(season.getIsDeleted())
                .build();
    }

    private int nextSeasonNumber(String seriesId) {
        return seasonRepository.findAllBySeries_SeriesIdAndIsDeletedFalseOrderBySeasonNumberAsc(seriesId)
                .stream()
                .map(Season::getSeasonNumber)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

}
