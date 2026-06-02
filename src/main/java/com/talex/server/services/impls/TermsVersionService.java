package com.talex.server.services.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.TermsVersionRequestDto;
import com.talex.server.dtos.requests.filters.TermVersionFilterRequestDto;
import com.talex.server.dtos.responses.TermsVersionResponseDto;
import com.talex.server.entities.TermsVersion;
import com.talex.server.enums.TermsType;
import com.talex.server.exceptions.codes.TermsVersionErrorCode;
import com.talex.server.exceptions.details.TermVersionException;
import com.talex.server.mappers.ITermsVersionMapper;
import com.talex.server.repositories.TermsVersionRepository;
import com.talex.server.services.ITermsVersionService;
import com.talex.server.specifications.TermsVersionSpec;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TermsVersionService implements ITermsVersionService {
    private final TermsVersionRepository repository;
    private final ITermsVersionMapper mapper;

    @Override
    public TermsVersionResponseDto create(TermsVersionRequestDto dto) {
        TermsVersion entity = mapper.toEntity(dto);
        TermsVersion saved = repository.save(entity);
        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TermsVersionResponseDto getById(String id) {
        return mapper.toResponseDto(findById(id));
    }

    @Override
    public TermsVersionResponseDto update(String id, TermsVersionRequestDto dto) {
        TermsVersion existing = findById(id);

        Optional.ofNullable(dto.getVersion()).ifPresent(existing::setVersion);
        Optional.ofNullable(dto.getType()).ifPresent(existing::setType);
        Optional.ofNullable(dto.getContent()).ifPresent(existing::setContent);

        TermsVersion saved = repository.save(existing);
        return mapper.toResponseDto(saved);
    }

    @Override
    public void delete(String id) {
        TermsVersion existing = findById(id);
        existing.setIsActive(Boolean.FALSE);
        repository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<TermsVersionResponseDto> list(TermVersionFilterRequestDto filterRequest) {
        int page = Optional.ofNullable(filterRequest.getPage()).orElse(0);
        int pageSize = Optional.ofNullable(filterRequest.getPageSize()).orElse(20);
        Sort sort = getSortBy(filterRequest);

        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);
        TermsType[] termsTypes = parseTypes(filterRequest.getTypes());

        Page<TermsVersion> resultPage = repository.findAll(TermsVersionSpec
                .filterByCriteria(filterRequest.getCriteria(), termsTypes), pageable);

        return BasePageResponse.<TermsVersionResponseDto>builder()
                .content(resultPage.map(mapper::toResponseDto).getContent())
                .pageNumber(resultPage.getNumber() + 1)
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .isFirst(resultPage.isFirst())
                .isLast(resultPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TermsVersionResponseDto getActiveByType(TermsType type) {
        return repository.findByTypeAndIsActiveTrue(type)
                .map(mapper::toResponseDto)
                .orElseThrow(() -> new TermVersionException(
                        TermsVersionErrorCode.ACTIVE_VERSION_NOT_FOUND,
                        "Active TermsVersion not found for type: " + type));
    }

    private TermsVersion findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new TermVersionException(
                        TermsVersionErrorCode.NOT_FOUND,
                        "TermsVersion not found with id: " + id));
    }

    private Sort getSortBy(BaseFilterRequestDto filterRequest) {
        String sortBy = Optional.ofNullable(filterRequest.getSortBy()).orElse("createdAt");
        String sortDirection = Optional.ofNullable(filterRequest.getSortDirection()).orElse("DESC");

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, normalizeSortProperty(sortBy));
    }

    private String normalizeSortProperty(String sortBy) {
        if (ValidationUtils.isNullOrEmpty(sortBy)) {
            return "createdAt";
        }

        return switch (sortBy) {
            case "version", "type", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }

    private TermsType[] parseTypes(String[] types) {
        if (types == null || types.length == 0) {
            return new TermsType[0];
        }

        TermsType[] termsTypes = new TermsType[types.length];
        for (int i = 0; i < types.length; i++) {
            try {
                termsTypes[i] = TermsType.valueOf(types[i].toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new TermVersionException(
                        TermsVersionErrorCode.TERMS_TYPE_INVALID,
                        "Invalid Term Types: " + types[i]
                );
            }
        }
        return termsTypes;
    }
}
