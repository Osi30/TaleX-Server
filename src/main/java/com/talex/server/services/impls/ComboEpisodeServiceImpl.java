package com.talex.server.services.impls;

import com.talex.server.dtos.requests.ComboEpisodeRequestDto;
import com.talex.server.dtos.responses.ComboEpisodeResponseDto;
import com.talex.server.entities.series.ComboEpisode;
import com.talex.server.entities.series.Episode;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.repositories.series.ComboEpisodeRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.services.ComboEpisodeService;
import com.talex.server.services.ContentOwnershipService;
import com.talex.server.services.audit.ContentAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComboEpisodeServiceImpl implements ComboEpisodeService {

    private final ComboEpisodeRepository comboEpisodeRepository;
    private final EpisodeRepository episodeRepository;
    private final ContentOwnershipService contentOwnershipService;
    private final ContentAuditLogger contentAuditLogger;

    @Override
    @Transactional
    public ComboEpisodeResponseDto create(ComboEpisodeRequestDto request, String accountId) {
        ComboEpisode combo = new ComboEpisode();
        combo.setTitle(request.getTitle());
        combo.setDescription(request.getDescription());
        combo.setPriceVnd(request.getPriceVnd());
        combo.setStatus(request.getStatus() != null ? request.getStatus() : EpisodeStatus.DRAFT);
        
        String creatorId = contentOwnershipService.requireCurrentCreatorId(accountId);
        combo.setCreatorId(creatorId);
        
        if (request.getEpisodeIds() != null && !request.getEpisodeIds().isEmpty()) {
            List<Episode> episodes = fetchAndValidateEpisodes(request.getEpisodeIds(), accountId);
            combo.setEpisodes(episodes);
        }
        
        combo = comboEpisodeRepository.save(combo);
        contentAuditLogger.logAction("ComboEpisode", combo.getComboId(), "CREATE", accountId, creatorId);
        return toResponse(combo);
    }

    @Override
    public ComboEpisodeResponseDto getById(String id, String accountId) {
        ComboEpisode combo = findActiveEntity(id);
        contentOwnershipService.assertCanManage(combo, accountId);
        return toResponse(combo);
    }

    @Override
    public List<ComboEpisodeResponseDto> listByCreator(String accountId) {
        String creatorId = contentOwnershipService.requireCurrentCreatorId(accountId);
        return comboEpisodeRepository.findByCreatorIdAndIsDeletedFalse(creatorId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ComboEpisodeResponseDto> getAll() {
        return comboEpisodeRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ComboEpisodeResponseDto update(String id, ComboEpisodeRequestDto request, String accountId) {
        ComboEpisode combo = findActiveEntity(id);
        contentOwnershipService.assertCanManage(combo, accountId);
        
        combo.setTitle(request.getTitle());
        combo.setDescription(request.getDescription());
        combo.setPriceVnd(request.getPriceVnd());
        if (request.getStatus() != null) {
            combo.setStatus(request.getStatus());
        }
        
        if (request.getEpisodeIds() != null) {
            List<Episode> episodes = fetchAndValidateEpisodes(request.getEpisodeIds(), accountId);
            combo.setEpisodes(episodes);
        } else {
            combo.getEpisodes().clear();
        }
        
        combo = comboEpisodeRepository.save(combo);
        contentAuditLogger.logAction("ComboEpisode", combo.getComboId(), "UPDATE", accountId, combo.getCreatorId());
        return toResponse(combo);
    }

    @Override
    @Transactional
    public void delete(String id, String accountId) {
        ComboEpisode combo = findActiveEntity(id);
        contentOwnershipService.assertCanManage(combo, accountId);
        combo.setIsDeleted(true);
        combo.softDelete();
        comboEpisodeRepository.save(combo);
        contentAuditLogger.logAction("ComboEpisode", combo.getComboId(), "DELETE", accountId, combo.getCreatorId());
    }

    private ComboEpisode findActiveEntity(String id) {
        ComboEpisode combo = comboEpisodeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Combo not found"));
        if (combo.getIsDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Combo not found");
        }
        return combo;
    }

    private List<Episode> fetchAndValidateEpisodes(List<String> episodeIds, String accountId) {
        List<Episode> episodes = episodeRepository.findAllById(episodeIds);
        if (episodes.size() != episodeIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some episodes not found");
        }
        for (Episode episode : episodes) {
            contentOwnershipService.assertCanManage(episode, accountId);
        }
        return episodes;
    }

    private ComboEpisodeResponseDto toResponse(ComboEpisode combo) {
        long originalPrice = 0L;
        List<ComboEpisodeResponseDto.EpisodeSummaryDto> summaryList = null;
        if (combo.getEpisodes() != null) {
            summaryList = combo.getEpisodes().stream().map(ep -> {
                ComboEpisodeResponseDto.EpisodeSummaryDto summary = new ComboEpisodeResponseDto.EpisodeSummaryDto();
                summary.setEpisodeId(ep.getEpisodeId());
                summary.setTitle(ep.getTitle());
                summary.setThumbnail(ep.getThumbnail());
                summary.setEpisodeNumber(ep.getEpisodeNumber());
                summary.setPriceVnd(ep.getPriceVnd());
                if (ep.getSeason() != null) {
                    summary.setSeasonId(ep.getSeason().getSeasonId());
                    summary.setSeasonTitle(ep.getSeason().getTitle());
                    if (ep.getSeason().getSeries() != null) {
                        summary.setSeriesTitle(ep.getSeason().getSeries().getTitle());
                    }
                }
                return summary;
            }).collect(Collectors.toList());
            
            originalPrice = combo.getEpisodes().stream().mapToLong(ep -> ep.getPriceVnd() != null ? ep.getPriceVnd() : 0L).sum();
        }

        return ComboEpisodeResponseDto.builder()
                .comboId(combo.getComboId())
                .creatorId(combo.getCreatorId())
                .title(combo.getTitle())
                .description(combo.getDescription())
                .status(combo.getStatus())
                .priceVnd(combo.getPriceVnd())
                .originalPriceVnd(originalPrice)
                .episodes(summaryList)
                .createdAt(combo.getCreatedAt())
                .updatedAt(combo.getUpdatedAt())
                .build();
    }
}
