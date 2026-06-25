package com.talex.server.services.impls;

import com.talex.server.dtos.requests.SeasonRequestDto;
import com.talex.server.dtos.responses.SeasonResponseDto;
import com.talex.server.entities.Season;
import com.talex.server.entities.Series;
import com.talex.server.enums.ContentApprovalStatus;
import com.talex.server.enums.SeasonStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.SeasonRepository;
import com.talex.server.services.SeasonService;
import com.talex.server.services.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonServiceImpl implements SeasonService {
    private final SeasonRepository seasonRepository;
    private final SeriesService seriesService;

    @Transactional
    @Override
    public SeasonResponseDto create(String seriesId, SeasonRequestDto request) {
        Series series = seriesService.findActiveEntity(seriesId);

        Season season = new Season();
        season.setSeries(series);
        season.setSeasonNumber(request.getSeasonNumber() != null
                ? request.getSeasonNumber()
                : nextSeasonNumber(seriesId));
        season.setTitle(request.getTitle());
        season.setDescription(request.getDescription());
        season.setStatus(SeasonStatus.DRAFT);
        season.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
        season.setApprovalReviewedAt(null);
        season.setApprovalReviewedBy(null);
        season.setScheduledPublishAt(null);
        season.markCreatedBy(request.getActorId());

        return toResponse(seasonRepository.save(season));
    }

    @Transactional(readOnly = true)
    @Override
    public SeasonResponseDto getById(String id) {
        return toResponse(findActiveEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public SeasonResponseDto getPublicById(String id) {
        return toResponse(findPublicEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<SeasonResponseDto> listBySeries(String seriesId) {
        seriesService.findActiveEntity(seriesId);
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
                .findAllBySeries_SeriesIdAndStatusAndApprovalStatusAndIsDeletedFalseOrderBySeasonNumberAsc(
                        seriesId,
                        SeasonStatus.PUBLISHED,
                        ContentApprovalStatus.APPROVED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public SeasonResponseDto update(String id, SeasonRequestDto request) {
        Season season = findActiveEntity(id);
        if (request.getSeasonNumber() != null) {
            season.setSeasonNumber(request.getSeasonNumber());
        }
        season.setTitle(request.getTitle());
        season.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            ensureApprovedForStatusUpdate(season.getApprovalStatus(), "Season");
            season.setStatus(request.getStatus());
        }
        season.markUpdatedBy(request.getActorId());

        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public SeasonResponseDto approve(String id, String actorId) {
        Season season = findActiveEntity(id);
        season.setApprovalStatus(ContentApprovalStatus.APPROVED);
        season.setApprovalReviewedAt(LocalDateTime.now());
        season.setApprovalReviewedBy(actorId);
        season.markUpdatedBy(actorId);
        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public SeasonResponseDto reject(String id, String actorId) {
        Season season = findActiveEntity(id);
        season.setApprovalStatus(ContentApprovalStatus.REJECTED);
        season.setApprovalReviewedAt(LocalDateTime.now());
        season.setApprovalReviewedBy(actorId);
        season.setScheduledPublishAt(null);
        if (season.getStatus() == SeasonStatus.PUBLISHED) {
            season.setStatus(SeasonStatus.HIDDEN);
        }
        season.markUpdatedBy(actorId);
        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public SeasonResponseDto publish(String id, String actorId) {
        Season season = findActiveEntity(id);
        ensureApprovedForStatusUpdate(season.getApprovalStatus(), "Season");
        season.setStatus(SeasonStatus.PUBLISHED);
        season.markUpdatedBy(actorId);
        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public SeasonResponseDto hide(String id, String actorId) {
        Season season = findActiveEntity(id);
        ensureApprovedForStatusUpdate(season.getApprovalStatus(), "Season");
        season.setStatus(SeasonStatus.HIDDEN);
        season.markUpdatedBy(actorId);
        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public SeasonResponseDto unhide(String id, String actorId) {
        Season season = findActiveEntity(id);
        ensureApprovedForStatusUpdate(season.getApprovalStatus(), "Season");
        season.setStatus(SeasonStatus.PUBLISHED);
        season.markUpdatedBy(actorId);
        return toResponse(seasonRepository.save(season));
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Season season = findActiveEntity(id);
        season.setStatus(SeasonStatus.DELETED);
        season.softDelete(actorId);
        seasonRepository.save(season);
    }

    @Override
    public Season findActiveEntity(String id) {
        return seasonRepository.findBySeasonIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Season not found: " + id));
    }

    @Override
    public Season findPublicEntity(String id) {
        Season season = findActiveEntity(id);
        if (season.getStatus() != SeasonStatus.PUBLISHED
                || season.getApprovalStatus() != ContentApprovalStatus.APPROVED) {
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
                .seasonNumber(season.getSeasonNumber())
                .title(season.getTitle())
                .description(season.getDescription())
                .status(season.getStatus())
                .approvalStatus(season.getApprovalStatus())
                .approvalReviewedAt(season.getApprovalReviewedAt())
                .approvalReviewedBy(season.getApprovalReviewedBy())
                .scheduledPublishAt(season.getScheduledPublishAt())
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

    private void ensureApprovedForStatusUpdate(ContentApprovalStatus approvalStatus, String entityName) {
        if (approvalStatus != ContentApprovalStatus.APPROVED) {
            throw ContentModuleException.badRequest(entityName + " must be approved before status can be updated");
        }
    }

}
