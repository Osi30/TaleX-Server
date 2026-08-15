package com.talex.server.services.campaign.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.campaign.request.CampaignRequestDto;
import com.talex.server.dtos.campaign.request.CampaignUpdateDto;
import com.talex.server.dtos.requests.filters.CampaignFilterRequestDto;
import com.talex.server.dtos.campaign.response.CampaignResponseDto;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.exceptions.codes.campaign.CampaignErrorCode;
import com.talex.server.exceptions.details.campaign.CampaignException;
import com.talex.server.mappers.campaign.CampaignMapper;
import com.talex.server.repositories.campaign.CampaignRepository;
import com.talex.server.repositories.campaign.CampaignSeriesRepository;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.services.campaign.CampaignService;
import com.talex.server.services.campaign.CampaignWalletService;
import com.talex.server.services.campaign.EngagementServiceService;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.specifications.campaign.CampaignSpec;
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
public class CampaignServiceImpl implements CampaignService {
    private final CampaignRepository campaignRepository;
    private final CampaignSeriesRepository campaignSeriesRepository;
    private final SeriesRepository seriesRepository;
    private final CreatorService creatorService;
    private final EngagementServiceService engagementService;
    private final CampaignWalletService campaignWalletService;
    private final CampaignMapper campaignMapper;

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
    public void validateCampaign(UUID accountId, List<String> seriesIds) {
        // 1. Kiểm tra nhanh số lượng tập phim hợp lệ
        long validSeriesCount = seriesRepository.countBySeriesIdInAndStatusAndIsDeletedFalseAndCreator_Account_AccountId(
                seriesIds,
                SeriesStatus.PUBLISHED,
                accountId
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

        if (!requestDto.getStatus().equals(CampaignStatus.CANCELLED)) {
            existing.updateStatus(requestDto.getStatus());
        }
        Optional.ofNullable(requestDto.getEndAt()).ifPresent(existing::setEndAt);

        Campaign updated = campaignRepository.save(existing);
        return campaignMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteCampaign(String campaignId) {
        Campaign campaign = findById(campaignId);
        if (campaign.getCampaignStatus().equals(CampaignStatus.RUNNING)
                || campaign.getCampaignStatus().equals(CampaignStatus.PAUSED)
        ) {
            campaign.updateStatus(CampaignStatus.CANCELLED);
            campaignRepository.save(campaign);
            campaignWalletService.refundCampaign(campaign);
        }
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
