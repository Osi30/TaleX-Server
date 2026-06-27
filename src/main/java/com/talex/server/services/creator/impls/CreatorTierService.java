package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorTierRequestDto;
import com.talex.server.dtos.requests.filters.CreatorTierFilterRequestDto;
import com.talex.server.dtos.responses.CreatorTierResponseDto;
import com.talex.server.entities.creator.CreatorTier;
import com.talex.server.exceptions.codes.CreatorTierErrorCode;
import com.talex.server.exceptions.details.CreatorTierException;
import com.talex.server.mappers.ICreatorTierMapper;
import com.talex.server.repositories.creator.CreatorTierRepository;
import com.talex.server.services.creator.ICreatorTierService;
import com.talex.server.specifications.CreatorTierSpec;
import com.talex.server.utils.PageUtils;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreatorTierService implements ICreatorTierService {
    private final CreatorTierRepository repository;
    private final ICreatorTierMapper mapper;

    @Override
    @Transactional
    public CreatorTierResponseDto create(CreatorTierRequestDto dto) {
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            dto.setTierLevel(0);
            dto.setMinFollowerRequired(0L);
            dto.setMinViewsRequired(0L);
            dto.setMinWatchTimeRequired(0.0);
        }

        CreatorTier entity = mapper.toEntity(dto);
        validateMonotonicConstraints(entity);

        CreatorTier saved = repository.save(entity);

        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            repository.unsetOtherDefaults(saved.getCreatorTierId());
        }

        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorTierResponseDto getById(String id) {
        return mapper.toResponseDto(findById(id));
    }

    @Override
    @Transactional
    public CreatorTierResponseDto update(String id, CreatorTierRequestDto dto) {
        CreatorTier existing = findById(id);

        Boolean willBeDefault = dto.getIsDefault() != null ? dto.getIsDefault() : existing.getIsDefault();
        if (Boolean.TRUE.equals(willBeDefault)) {
            dto.setIsDefault(true);
            dto.setTierLevel(0);
            dto.setMinFollowerRequired(0L);
            dto.setMinViewsRequired(0L);
            dto.setMinWatchTimeRequired(0.0);
        }

        mapper.updateEntity(dto, existing);
        validateMonotonicConstraints(existing);

        CreatorTier saved = repository.save(existing);

        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            repository.unsetOtherDefaults(saved.getCreatorTierId());
        }

        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        CreatorTier existing = findById(id);
        existing.setIsDeleted(true);
        repository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<CreatorTierResponseDto> list(CreatorTierFilterRequestDto filterRequest) {
        Sort sort = buildSort(filterRequest);
        Pageable pageable = PageUtils.buildPageable(filterRequest.getPage(), filterRequest.getPageSize(), sort);

        Page<CreatorTier> pageResult = repository.findAll(
                CreatorTierSpec.filterByCriteria(filterRequest.getCriteria()),
                pageable
        );

        List<CreatorTierResponseDto> content = pageResult.stream()
                .map(mapper::toResponseDto)
                .toList();

        return BasePageResponse.<CreatorTierResponseDto>builder()
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
    public CreatorTier findById(String id) {
        return repository.findByCreatorTierIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new CreatorTierException(CreatorTierErrorCode.NOT_FOUND,
                        "Không tìm thấy cấp độ creator với id: " + id));
    }

    private void validateMonotonicConstraints(CreatorTier target) {
        List<CreatorTier> allTiers = new ArrayList<>(repository.findAllByIsDeletedFalseOrderByTierLevelAsc());

        boolean found = false;
        for (int i = 0; i < allTiers.size(); i++) {
            CreatorTier current = allTiers.get(i);
            if (target.getCreatorTierId() != null && target.getCreatorTierId().equals(current.getCreatorTierId())) {
                allTiers.set(i, target);
                found = true;
                break;
            }
        }
        if (!found) {
            allTiers.add(target);
        }

        allTiers.sort(Comparator.comparingInt(CreatorTier::getTierLevel));

        for (int i = 0; i < allTiers.size() - 1; i++) {
            CreatorTier lower = allTiers.get(i);
            CreatorTier higher = allTiers.get(i + 1);

            if (lower.getTierLevel().equals(higher.getTierLevel())) {
                throw new CreatorTierException(CreatorTierErrorCode.LEVEL_ALREADY_EXISTS,
                        "Cấp độ creator tier " + lower.getTierLevel() + " bị trùng lặp.");
            }

            if (higher.getMinFollowerRequired() <= lower.getMinFollowerRequired()) {
                throw new CreatorTierException(CreatorTierErrorCode.INVALID_REQUEST,
                        String.format("Yêu cầu follower của cấp độ %d (%d) phải cao hơn cấp độ %d (%d).",
                                higher.getTierLevel(), higher.getMinFollowerRequired(),
                                lower.getTierLevel(), lower.getMinFollowerRequired()));
            }

            if (higher.getMinViewsRequired() <= lower.getMinViewsRequired()) {
                throw new CreatorTierException(CreatorTierErrorCode.INVALID_REQUEST,
                        String.format("Yêu cầu lượt xem của cấp độ %d (%d) phải cao hơn cấp độ %d (%d).",
                                higher.getTierLevel(), higher.getMinViewsRequired(),
                                lower.getTierLevel(), lower.getMinViewsRequired()));
            }

            if (higher.getMinWatchTimeRequired() <= lower.getMinWatchTimeRequired()) {
                throw new CreatorTierException(CreatorTierErrorCode.INVALID_REQUEST,
                        String.format("Yêu cầu thời gian xem của cấp độ %d (%.1f) phải cao hơn cấp độ %d (%.1f).",
                                higher.getTierLevel(), higher.getMinWatchTimeRequired(),
                                lower.getTierLevel(), lower.getMinWatchTimeRequired()));
            }
        }
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
            case "tierName", "tierLevel", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }
}
