package com.talex.server.services.campaign.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.EngagementServiceRequestDto;
import com.talex.server.dtos.requests.filters.EngagementServiceFilterRequestDto;
import com.talex.server.dtos.responses.campaign.EngagementServiceResponseDto;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.enums.engagement.EngagementTarget;
import com.talex.server.enums.engagement.EngagementType;
import com.talex.server.exceptions.codes.EngagementErrorCode;
import com.talex.server.exceptions.details.EngagementServiceException;
import com.talex.server.mappers.campaign.IEngagementServiceMapper;
import com.talex.server.repositories.campaign.EngagementServiceRepository;
import com.talex.server.services.campaign.IEngagementServiceService;
import com.talex.server.specifications.EngagementServiceSpec;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EngagementServiceService implements IEngagementServiceService {
    private final EngagementServiceRepository engagementServiceRepository;
    private final IEngagementServiceMapper engagementServiceMapper;

    @Override
    @Transactional
    public EngagementServiceResponseDto createEngagementService(EngagementServiceRequestDto requestDto) {
        EngagementService entity = engagementServiceMapper.toEntity(requestDto);
        EngagementService saved = engagementServiceRepository.save(entity);
        return engagementServiceMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<EngagementServiceResponseDto> filterEngagementServices(
            EngagementServiceFilterRequestDto filterRequest
    ) {
        int page = Optional.ofNullable(filterRequest.getPage()).orElse(1);
        int pageSize = Optional.ofNullable(filterRequest.getPageSize()).orElse(20);
        if (page < 1) {
            page = 1;
        }

        Sort sort = buildSort(filterRequest);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        EngagementType[] types = parseTypes(filterRequest.getTypes());
        EngagementTarget[] targets = parseTargets(filterRequest.getTargets());

        Page<EngagementService> pageResult = engagementServiceRepository.findAll(
                EngagementServiceSpec.filterByCriteria(filterRequest.getCriteria(), types, targets),
                pageable
        );

        List<EngagementServiceResponseDto> content = pageResult.stream()
                .map(engagementServiceMapper::toResponseDto)
                .toList();

        return BasePageResponse.<EngagementServiceResponseDto>builder()
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
    public EngagementServiceResponseDto getEngagementServiceById(String engagementServiceId) {
        return engagementServiceMapper.toResponseDto(findById(engagementServiceId));
    }

    @Override
    @Transactional
    public EngagementServiceResponseDto updateEngagementService(
            String engagementServiceId,
            EngagementServiceRequestDto requestDto
    ) {
        EngagementService entity = findById(engagementServiceId);
        engagementServiceMapper.updateEntity(requestDto, entity);
        EngagementService updated = engagementServiceRepository.save(entity);
        return engagementServiceMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteEngagementService(String engagementServiceId) {
        EngagementService entity = findById(engagementServiceId);
        entity.setIsDeleted(true);
        engagementServiceRepository.save(entity);
    }

    private EngagementService findById(String id) {
        return engagementServiceRepository.findById(id)
                .orElseThrow(() -> new EngagementServiceException(EngagementErrorCode.NOT_FOUND, "EngagementService not found with id: " + id));
    }

    private Sort buildSort(BaseFilterRequestDto filterRequest) {
        String sortDirection = Optional.ofNullable(filterRequest.getSortDirection()).orElse("DESC");
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return Sort.by(direction, normalizeSortProperty(filterRequest.getSortBy()));
    }

    private String normalizeSortProperty(String sortBy) {
        if (ValidationUtils.isNullOrEmpty(sortBy)) {
            return "createdAt";
        }
        return switch (sortBy) {
            case "name", "price", "targetValue", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }

    private EngagementType[] parseTypes(String[] types) {
        if (types == null || types.length == 0) {
            return new EngagementType[0];
        }
        EngagementType[] parsed = new EngagementType[types.length];
        for (int i = 0; i < types.length; i++) {
            try {
                parsed[i] = EngagementType.valueOf(types[i].toUpperCase());
            } catch (Exception e) {
                throw new EngagementServiceException(EngagementErrorCode.TYPE_INVALID, "Loại tương tác không hợp lệ: " + types[i]);
            }
        }
        return parsed;
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
                throw new EngagementServiceException(EngagementErrorCode.TARGET_INVALID, "Đối tượng tương tác không hợp lệ: " + targets[i]);
            }
        }
        return parsed;
    }
}
