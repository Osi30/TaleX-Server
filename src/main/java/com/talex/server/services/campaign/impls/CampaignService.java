package com.talex.server.services.campaign.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.CampaignRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignResponseDto;
import com.talex.server.entities.Account;
import com.talex.server.entities.Creator;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.mappers.campaign.ICampaignMapper;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.campaign.CampaignRepository;
import com.talex.server.repositories.campaign.EngagementServiceRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.services.campaign.ICampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignService implements ICampaignService {
    private final CampaignRepository campaignRepository;
    private final AccountRepository accountRepository;
    private final CreatorRepository creatorRepository;
    private final EngagementServiceRepository engagementServiceRepository;
    private final ICampaignMapper campaignMapper;

    @Override
    @Transactional
    public CampaignResponseDto createCampaign(CampaignRequestDto requestDto) {
        Account account = fetchAccount(requestDto.getAccountId());
        Creator creator = fetchCreator(requestDto.getCreatorId());
        EngagementService service = fetchEngagementService(requestDto.getEngagementServiceId());

        Campaign campaign = campaignMapper.toEntity(requestDto);
        campaign.setCreator(creator);
        campaign.setEngagementService(service);

        Campaign saved = campaignRepository.save(campaign);
        return campaignMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<CampaignResponseDto> filterCampaigns(String[] statuses, String[] types,
            java.util.Map<String, Object> criteria, String sortBy, String sortDirection, Integer page,
            Integer pageSize) {
        int validatedPage = Optional.ofNullable(page).orElse(1);
        int validatedPageSize = Optional.ofNullable(pageSize).orElse(20);

        Sort sort = buildSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(validatedPage - 1, validatedPageSize, sort);

        Specification<Campaign> specification = Specification.unrestricted();

        Page<Campaign> pageResult = campaignRepository.findAll(specification, pageable);
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
    public CampaignResponseDto updateCampaign(String campaignId, CampaignRequestDto requestDto) {
        Campaign existing = findById(campaignId);

        if (requestDto.getCreatorId() != null) {
            existing.setCreator(fetchCreator(requestDto.getCreatorId()));
        }
        if (requestDto.getEngagementServiceId() != null) {
            existing.setEngagementService(fetchEngagementService(requestDto.getEngagementServiceId()));
        }

        campaignMapper.updateEntity(requestDto, existing);
        Campaign updated = campaignRepository.save(existing);
        return campaignMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteCampaign(String campaignId) {
        Campaign campaign = findById(campaignId);
        campaignRepository.delete(campaign);
    }

    private Campaign findById(String id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
    }

    private Account fetchAccount(String accountId) {
        return accountRepository.findById(java.util.UUID.fromString(accountId))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private Creator fetchCreator(String creatorId) {
        return creatorRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found with id: " + creatorId));
    }

    private EngagementService fetchEngagementService(String engagementServiceId) {
        return engagementServiceRepository.findById(engagementServiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EngagementService not found with id: " + engagementServiceId));
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (sortBy == null || sortBy.isEmpty()) {
            return Sort.by(direction, "createdAt");
        }
        return switch (sortBy) {
            case "name", "status", "startAt", "endAt", "budget", "createdAt", "updatedAt" -> Sort.by(direction, sortBy);
            default -> Sort.by(direction, "createdAt");
        };
    }
}
