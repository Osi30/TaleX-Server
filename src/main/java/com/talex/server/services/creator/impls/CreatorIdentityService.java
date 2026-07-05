package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorIdentityRequestDto;
import com.talex.server.dtos.requests.creator.CreatorVerifiedResultDto;
import com.talex.server.dtos.requests.filters.CreatorIdentityFilterRequestDto;
import com.talex.server.dtos.responses.CreatorIdentityResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorIdentity;
import com.talex.server.enums.creator.CreatorIdentityStatus;
import com.talex.server.exceptions.codes.CreatorIdentityErrorCode;
import com.talex.server.exceptions.details.CreatorIdentityException;
import com.talex.server.mappers.ICreatorIdentityMapper;
import com.talex.server.repositories.creator.CreatorIdentityRepository;
import com.talex.server.services.creator.ICreatorIdentityService;
import com.talex.server.specifications.CreatorIdentitySpec;
import com.talex.server.utils.PageUtils;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatorIdentityService implements ICreatorIdentityService {
    private final CreatorIdentityRepository repository;
    private final ICreatorIdentityMapper mapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void create(Creator creator) {
        // Check if Creator already has an identity
        Optional<CreatorIdentity> creatorIdentity = repository
                .findByCreator_CreatorId(creator.getCreatorId());
        if (creatorIdentity.isPresent()) {
            return;
        }

        CreatorIdentity entity = CreatorIdentity.builder()
                .creator(creator)
                .status(CreatorIdentityStatus.AWAITING_FILL)
                .build();

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorIdentityResponseDto getById(String id) {
        return mapper.toResponseDto(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorIdentityResponseDto getByAccountId(String accountId) {
        CreatorIdentity entity = findByAccountId(accountId);
        return mapper.toResponseDto(entity);
    }

    @Override
    public CreatorIdentityResponseDto update(String id, CreatorIdentityRequestDto dto) {
        CreatorIdentity existing = findById(id);

        Optional.ofNullable(dto.getIdNumber()).ifPresent(existing::setIdNumber);
        Optional.ofNullable(dto.getFullName()).ifPresent(existing::setFullName);
        Optional.ofNullable(dto.getDob()).ifPresent(existing::setDob);
        Optional.ofNullable(dto.getSex()).ifPresent(existing::setSex);
        Optional.ofNullable(dto.getAddress()).ifPresent(existing::setAddress);
        Optional.ofNullable(dto.getDoe()).ifPresent(existing::setDoe);
        Optional.ofNullable(dto.getTaxId()).ifPresent(existing::setTaxId);

        CreatorIdentity saved = repository.save(existing);
        return mapper.toResponseDto(saved);
    }

    @Override
    public void updateVerifiedStatus(String id, CreatorVerifiedResultDto dto) {
        CreatorIdentity existing = findById(id);

        Optional.ofNullable(dto.getStatus()).ifPresent(e -> {
            existing.setStatus(e);
            if (e.equals(CreatorIdentityStatus.APPROVED)) {
                existing.setVerifiedAt(LocalDateTime.now());
            }
        });
        Optional.ofNullable(dto.getVerifiedNote()).ifPresent(existing::setVerifiedNote);

        if (existing.getStatus().equals(CreatorIdentityStatus.APPROVED)) {
            kafkaTemplate.send("request-to-update-account", existing.getCreator().getAccount().getAccountId().toString());
        }
        repository.save(existing);
    }

    @Override
    public String updateTaxId(UUID accountId, String taxId) {
        if (ValidationUtils.isNullOrEmpty(taxId)) {
            throw new CreatorIdentityException(CreatorIdentityErrorCode.INVALID_TAX_ID);
        }

        CreatorIdentity creatorIdentity = findByAccountId(accountId.toString());
        creatorIdentity.setTaxId(taxId);
        creatorIdentity.setStatus(CreatorIdentityStatus.PENDING);
        repository.save(creatorIdentity);
        return creatorIdentity.getStatus().toString();
    }

    @Override
    public void delete(String id) {
        CreatorIdentity existing = findById(id);
        repository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<CreatorIdentityResponseDto> filter(CreatorIdentityFilterRequestDto filterRequest) {
        Sort sort = buildSort(filterRequest);
        Pageable pageable = PageUtils.buildPageable(filterRequest.getPage(), filterRequest.getPageSize(), sort);
        CreatorIdentityStatus[] statuses = parseStatuses(filterRequest.getStatuses());

        // Gọi truy vấn kèm theo Specification động
        Page<CreatorIdentity> pageResult = repository.findAll(
                CreatorIdentitySpec.filterByCriteria(filterRequest.getCriteria(), statuses),
                pageable
        );

        List<CreatorIdentityResponseDto> content = pageResult.stream()
                .map(mapper::toResponseDto)
                .toList();

        return BasePageResponse.<CreatorIdentityResponseDto>builder()
                .content(content)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    private CreatorIdentity findById(String id) {
        return repository.findById(id)
                .orElseThrow(
                        () -> new CreatorIdentityException(
                                CreatorIdentityErrorCode.CREATOR_IDENTITY_NOT_FOUND,
                                "CreatorIdentity not found for creator: " + id)
                );
    }

    private CreatorIdentity findByAccountId(String accountId) {
        return repository.findByCreator_Account_AccountId(UUID.fromString(accountId))
                .orElseThrow(
                        () -> new CreatorIdentityException(
                                CreatorIdentityErrorCode.CREATOR_IDENTITY_NOT_FOUND,
                                "CreatorIdentity not found for account: " + accountId)
                );
    }

    private Sort buildSort(CreatorIdentityFilterRequestDto filterRequest) {
        String sortDirection = Optional.ofNullable(filterRequest.getSortDirection()).orElse("DESC");
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, normalizeSortProperty(filterRequest.getSortBy()));
    }

    private String normalizeSortProperty(String sortBy) {
        if (ValidationUtils.isNullOrEmpty(sortBy)) {
            return "createdAt";
        }
        return switch (sortBy) {
            case "verifiedAt", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }

    private CreatorIdentityStatus[] parseStatuses(String[] statuses) {
        if (statuses == null || statuses.length == 0) {
            return new CreatorIdentityStatus[0];
        }
        CreatorIdentityStatus[] parsed = new CreatorIdentityStatus[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            try {
                parsed[i] = CreatorIdentityStatus.valueOf(statuses[i].toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái danh tính không hợp lệ: " + statuses[i]);
            }
        }
        return parsed;
    }
}
