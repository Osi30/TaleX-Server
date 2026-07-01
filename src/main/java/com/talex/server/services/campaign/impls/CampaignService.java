package com.talex.server.services.campaign.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.CampaignRequestDto;
import com.talex.server.dtos.requests.campaign.CampaignUpdateDto;
import com.talex.server.dtos.requests.filters.CampaignFilterRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignResponseDto;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.entities.series.Episode;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.enums.engagement.EngagementTarget;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.exceptions.codes.CampaignErrorCode;
import com.talex.server.exceptions.details.CampaignException;
import com.talex.server.mappers.campaign.ICampaignMapper;
import com.talex.server.repositories.campaign.CampaignRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.services.campaign.ICampaignService;
import com.talex.server.services.campaign.IEngagementServiceService;
import com.talex.server.services.creator.ICreatorService;
import com.talex.server.specifications.CampaignSpec;
import com.talex.server.utils.PageUtils;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CampaignService implements ICampaignService {
    private final CampaignRepository campaignRepository;
    private final EpisodeRepository episodeRepository;
    private final ICreatorService creatorService;
    private final IEngagementServiceService engagementService;
    private final ICampaignMapper campaignMapper;

    @Override
    @Transactional
    public CampaignResponseDto createCampaign(CampaignRequestDto requestDto) {
        // 1. Loại bỏ các ID trùng lặp
        Set<String> uniqueEpisodeIds = new HashSet<>(requestDto.getEpisodeIds());
        if (uniqueEpisodeIds.isEmpty()) {
            throw new CampaignException(CampaignErrorCode.INVALID_REQUEST, "Danh sách tập phim không được để trống");
        }

        // 2. Kiểm tra nhanh số lượng tập phim hợp lệ
        String creatorId = creatorService.getIdByAccountId(requestDto.getAccountId());
        long validEpisodesCount = episodeRepository.countByEpisodeIdInAndStatusAndIsDeletedFalseAndCreatorId(
                uniqueEpisodeIds,
                EpisodeStatus.PUBLISHED,
                creatorId
        );
        // Nếu số lượng trong DB không khớp với số lượng ID
        if (validEpisodesCount != uniqueEpisodeIds.size()) {
            throw new CampaignException(CampaignErrorCode.INVALID_REQUEST, "Có tập phim không hợp lệ hoặc chưa được xuất bản");
        }

        // 3. Lấy thông tin dịch vụ tương tác
        EngagementService service = engagementService.findById(requestDto.getEngagementServiceId());

        // 4. Khởi tạo Campaign
        Campaign campaign = new Campaign();
        campaign.setAccountId(requestDto.getAccountId());
        campaign.setOrderId(requestDto.getOrderId());
        campaign.setEngagementService(service);
        campaign.setTargetValue(service.getTargetValue());
        campaign.setEngagementTarget(service.getEngagementTarget());
        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setStartAt(LocalDateTime.now());

        // 5. Liên kết các Episode vào Campaign (không chọc DB)
        for (String episodeId : uniqueEpisodeIds) {
            // getReferenceById tạo ra một Hibernate Proxy object
            Episode episodeProxy = episodeRepository.getReferenceById(episodeId);
            campaign.addEpisode(episodeProxy);
        }

        Campaign saved = campaignRepository.save(campaign);
        return campaignMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<CampaignResponseDto> filterCampaigns(CampaignFilterRequestDto filterRequest) {
        Sort sort = buildSort(filterRequest);
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(), sort);

        EngagementTarget[] targets = parseTargets(filterRequest.getTargets());
        CampaignStatus[] statuses = parseStatuses(filterRequest.getStatuses());

        Page<Campaign> pageResult = campaignRepository.findAll(
                CampaignSpec.filterByCriteria(filterRequest.getCriteria(), targets, statuses),
                pageable
        );

        List<CampaignResponseDto> content = pageResult.stream()
                .map(campaignMapper::toResponseDto)
                .toList();

        return BasePageResponse.<CampaignResponseDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponseDto getCampaignById(String campaignId) {
        return campaignMapper.toResponseDto(findById(campaignId));
    }

    @Override
    @Transactional
    public CampaignResponseDto updateCampaign(String campaignId, CampaignUpdateDto requestDto) {
        Campaign existing = findById(campaignId);

        Optional.ofNullable(requestDto.getCurrentValue()).ifPresent(existing::setCurrentValue);

        Optional.ofNullable(requestDto.getStatus()).ifPresent(s -> {
            if (s.equals(CampaignStatus.CANCELLED)) {
                return;
            }
            existing.setStatus(s);
        });
        Optional.ofNullable(requestDto.getEndAt()).ifPresent(existing::setEndAt);

        Campaign updated = campaignRepository.save(existing);
        return campaignMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteCampaign(String campaignId) {
        Campaign campaign = findById(campaignId);
        switch (campaign.getStatus()) {
            case CANCELLED:
                // Do nothing
                break;
            case RUNNING:
                campaign.setStatus(CampaignStatus.CANCELLED);
                break;
            case PAUSED:
                campaign.setStatus(CampaignStatus.CANCELLED);
                break;
            case FAILED:
                // Do nothing
                break;
            case COMPLETED:
                // Do nothing
                break;
        }
        campaignRepository.save(campaign);
    }

    private Campaign findById(String id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new CampaignException(CampaignErrorCode.NOT_FOUND, "Campaign not found with id: " + id));
    }

    private Sort buildSort(CampaignFilterRequestDto filterRequest) {
        String sortDirection = Optional.ofNullable(filterRequest.getSortDirection()).orElse("DESC");
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, normalizeSortProperty(filterRequest.getSortBy()));
    }

    private String normalizeSortProperty(String sortBy) {
        if (ValidationUtils.isNullOrEmpty(sortBy)) {
            return "createdAt";
        }
        return switch (sortBy) {
            case "startAt", "endAt", "currentValue", "targetValue", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }

    private EngagementTarget[] parseTargets(String[] targets) {
        if (targets == null || targets.length == 0) {
            return new EngagementTarget[0];
        }
        EngagementTarget[] parsed = new EngagementTarget[targets.length];
        for (int i = 0; i < targets.length; i++) {
            try {
                parsed[i] = EngagementTarget.valueOf(targets[i].toUpperCase());
            } catch (Exception e) {
                throw new CampaignException(CampaignErrorCode.INVALID_REQUEST, "Mục tiêu tương tác không hợp lệ: " + targets[i]);
            }
        }
        return parsed;
    }

    private CampaignStatus[] parseStatuses(String[] statuses) {
        if (statuses == null || statuses.length == 0) {
            return new CampaignStatus[0];
        }
        CampaignStatus[] parsed = new CampaignStatus[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            try {
                parsed[i] = CampaignStatus.valueOf(statuses[i].toUpperCase());
            } catch (Exception e) {
                throw new CampaignException(CampaignErrorCode.INVALID_REQUEST, "Trạng thái chiến dịch không hợp lệ: " + statuses[i]);
            }
        }
        return parsed;
    }
}
