package com.talex.server.services.campaign.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.CampaignRequestDto;
import com.talex.server.dtos.requests.campaign.CampaignUpdateDto;
import com.talex.server.dtos.requests.filters.CampaignFilterRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignResponseDto;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.exceptions.codes.CampaignErrorCode;
import com.talex.server.exceptions.details.CampaignException;
import com.talex.server.mappers.campaign.ICampaignMapper;
import com.talex.server.repositories.campaign.CampaignRepository;
import com.talex.server.repositories.campaign.CampaignSeriesRepository;
import com.talex.server.repositories.series.SeriesRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignService implements ICampaignService {
    private final CampaignRepository campaignRepository;
    private final CampaignSeriesRepository campaignSeriesRepository;
    private final SeriesRepository seriesRepository;
    private final ICreatorService creatorService;
    private final IEngagementServiceService engagementService;
    private final ICampaignMapper campaignMapper;

    @Override
    @Transactional
    public CampaignResponseDto createCampaign(CampaignRequestDto requestDto) {
        // 1. Loại bỏ các ID trùng lặp
        Set<String> uniqueSeriesIds = new HashSet<>(requestDto.getSeriesIds());
        if (uniqueSeriesIds.isEmpty()) {
            throw new CampaignException(CampaignErrorCode.INVALID_REQUEST, "Danh sách series không được để trống");
        }

        // 2. Lấy thông tin dịch vụ tương tác
        EngagementService service = engagementService.findActive(requestDto.getEngagementServiceId());

        // 3. Khởi tạo Campaign
        Campaign campaign = new Campaign();
        campaign.setAccountId(requestDto.getAccountId());
        campaign.setOrderId(requestDto.getOrderId());
        campaign.setEngagementService(service);
        campaign.setEngagementType(service.getEngagementType());
        campaign.setTargetImpression(service.getTargetValue());
        campaign.setStartAt(LocalDateTime.now());

        // 4. Liên kết các Series vào Campaign
        for (String seriesId : uniqueSeriesIds) {
            Series seriesProxy = seriesRepository.getReferenceById(seriesId);
            campaign.addSeries(seriesProxy);
        }

        Campaign saved = campaignRepository.save(campaign);
        return campaignMapper.toResponseDto(saved);
    }

    @Override
    public void validateCampaign(UUID accountId, List<String> seriesIds){
        // 1. Kiểm tra nhanh số lượng tập phim hợp lệ
        String creatorId = creatorService.getIdByAccountId(accountId);
        long validSeriesCount = seriesRepository.countBySeriesIdInAndStatusAndIsDeletedFalseAndCreator_CreatorId(
                seriesIds,
                SeriesStatus.PUBLISHED,
                creatorId
        );
        // Nếu số lượng trong DB không khớp với số lượng ID
        if (validSeriesCount != seriesIds.size()) {
            throw new CampaignException(CampaignErrorCode.INVALID_REQUEST, "Có series không hợp lệ hoặc chưa được xuất bản");
        }

        // 2. Kiểm tra đã có trong campaign nào trước chưa
        List<String> duplicatedSeries = campaignSeriesRepository.findDuplicatedPromotedSeriesIds(
                List.of(CampaignStatus.RUNNING, CampaignStatus.PAUSED),
                seriesIds
        );
        if (!duplicatedSeries.isEmpty()) {
            String duplicateSeries = duplicatedSeries.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new CampaignException(
                    CampaignErrorCode.INVALID_REQUEST,
                    "Phát hiện có series đang sử dụng dịch vụ: " + duplicateSeries);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<CampaignResponseDto> filterCampaigns(CampaignFilterRequestDto filterRequest) {
        Sort sort = buildSort(filterRequest);
        Pageable pageable = PageUtils.buildPageable(
                filterRequest.getPage(), filterRequest.getPageSize(), sort);

        Page<Campaign> pageResult = campaignRepository.findAll(
                CampaignSpec.filterByCriteria(filterRequest.getCriteria()),
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

        Optional.ofNullable(requestDto.getStatus()).ifPresent(existing::updateStatus);
        Optional.ofNullable(requestDto.getEndAt()).ifPresent(existing::setEndAt);

        Campaign updated = campaignRepository.save(existing);
        return campaignMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteCampaign(String campaignId) {
        Campaign campaign = findById(campaignId);
        campaign.updateStatus(CampaignStatus.CANCELLED);
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
            case "startAt", "endAt", "currentImpression", "targetImpression", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }
}
