package com.talex.server.services.campaign.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.CampaignLogRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignLogResponseDto;
import com.talex.server.entities.Account;
import com.talex.server.entities.campaign.Campaign;
import com.talex.server.entities.campaign.CampaignLog;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.mappers.campaign.ICampaignLogMapper;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.campaign.CampaignLogRepository;
import com.talex.server.repositories.campaign.CampaignRepository;
import com.talex.server.services.campaign.ICampaignLogService;
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
public class CampaignLogService implements ICampaignLogService {
    private final CampaignLogRepository campaignLogRepository;
    private final CampaignRepository campaignRepository;
    private final AccountRepository accountRepository;
    private final ICampaignLogMapper campaignLogMapper;

    @Override
    @Transactional
    public CampaignLogResponseDto createCampaignLog(CampaignLogRequestDto requestDto) {
        Campaign campaign = fetchCampaign(requestDto.getCampaignId());
        Account account = fetchAccount(requestDto.getAccountId());

        CampaignLog campaignLog = campaignLogMapper.toEntity(requestDto);
        campaignLog.setCampaign(campaign);

        CampaignLog saved = campaignLogRepository.save(campaignLog);
        return campaignLogMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<CampaignLogResponseDto> filterCampaignLogs(String[] eventTypes,
            java.util.Map<String, Object> criteria, String sortBy, String sortDirection, Integer page,
            Integer pageSize) {
        int validatedPage = Optional.ofNullable(page).orElse(1);
        int validatedPageSize = Optional.ofNullable(pageSize).orElse(20);

        Sort sort = buildSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(validatedPage - 1, validatedPageSize, sort);

        Specification<CampaignLog> specification = Specification.unrestricted();

        Page<CampaignLog> pageResult = campaignLogRepository.findAll(specification, pageable);
        List<CampaignLogResponseDto> content = pageResult.stream()
                .map(campaignLogMapper::toResponseDto)
                .toList();

        return BasePageResponse.<CampaignLogResponseDto>builder()
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
    public CampaignLogResponseDto getCampaignLogById(String campaignLogId) {
        return campaignLogMapper.toResponseDto(findById(campaignLogId));
    }

    @Override
    @Transactional
    public void deleteCampaignLog(String campaignLogId) {
        CampaignLog campaignLog = findById(campaignLogId);
        campaignLogRepository.delete(campaignLog);
    }

    private CampaignLog findById(String id) {
        return campaignLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CampaignLog not found with id: " + id));
    }

    private Campaign fetchCampaign(String campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));
    }

    private Account fetchAccount(String accountId) {
        return accountRepository.findById(java.util.UUID.fromString(accountId))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (sortBy == null || sortBy.isEmpty()) {
            return Sort.by(direction, "createdAt");
        }
        return switch (sortBy) {
            case "eventType", "createdAt" -> Sort.by(direction, sortBy);
            default -> Sort.by(direction, "createdAt");
        };
    }
}
