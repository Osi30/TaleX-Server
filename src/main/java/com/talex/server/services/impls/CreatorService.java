package com.talex.server.services.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.CreatorRegisterDto;
import com.talex.server.dtos.requests.CreatorRequestDto;
import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.requests.filters.CreatorFilterRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.entities.Account;
import com.talex.server.entities.Creator;
import com.talex.server.exceptions.details.CreatorException;
import com.talex.server.exceptions.codes.CreatorErrorCode;
import com.talex.server.mappers.ICreatorMapper;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.CreatorRepository;
import com.talex.server.services.ICreatorIdentityService;
import com.talex.server.services.ICreatorService;
import com.talex.server.services.ITermsLogService;
import com.talex.server.services.IKycSessionService;
import com.talex.server.specifications.CreatorSpec;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatorService implements ICreatorService {
    private final ITermsLogService creatorTermsLogService;
    private final ICreatorIdentityService creatorIdentityService;
    private final IKycSessionService kycSessionService;
    private final CreatorRepository creatorRepository;
    private final AccountRepository accountRepository;
    private final ICreatorMapper creatorMapper;

    @Override
    @Transactional
    public String createCreator(CreatorRegisterDto dto) {
        Creator creator;

        // Đã đồng ý điều khoản
        if (ValidationUtils.isNullOrEmpty(dto.getTermsId())) {
            creator = findCreatorByAccountId(dto.getAccountId().toString());
        }
        // Chưa đồng ý điều khoản
        else {
            // 1. Creator
            Account account = accountRepository.findById(dto.getAccountId()).orElseThrow(
                    () -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND));
            creator = creatorRepository.save(Creator.builder()
                    .account(account)
                    .build());

            // 2. Log
            creatorTermsLogService.create(CreatorTermsLogRequestDto.builder()
                    .versionId(dto.getTermsId())
                    .creator(creator)
                    .build());

            // 3. Identity
            creatorIdentityService.create(creator);
        }

        // 4. Session
        return kycSessionService.createSession(creator);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorResponseDto getById(String id) {
        Creator creator = findById(id);
        return creatorMapper.toResponseDto(creator);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<CreatorResponseDto> filterCreators(CreatorFilterRequestDto filterRequest) {
        int page = Optional.ofNullable(filterRequest.getPage()).orElse(1);
        int pageSize = Optional.ofNullable(filterRequest.getPageSize()).orElse(20);
        Sort sort = getSort(filterRequest);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Page<Creator> pageResult = creatorRepository.findAll(CreatorSpec.filterByCriteria(filterRequest), pageable);
        return BasePageResponse.<CreatorResponseDto>builder()
                .content(pageResult.map(creatorMapper::toResponseDto).getContent())
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isFirst(pageResult.isFirst())
                .isLast(pageResult.isLast())
                .build();
    }

    @Override
    public CreatorResponseDto getByAccount(UUID accountId) {
        return creatorMapper.toResponseDto(
                findCreatorByAccountId(accountId.toString()));
    }

    @Override
    public CreatorResponseDto updateCreator(String id, CreatorRequestDto dto) {
        Creator existing = findById(id);

        existing.setNickname(dto.getNickname());
        existing.setBio(dto.getBio());

        Creator saved = creatorRepository.save(existing);
        return creatorMapper.toResponseDto(saved);
    }

    @Override
    public void deleteCreator(String id) {
        Creator existing = findById(id);
        creatorRepository.delete(existing);
    }

    private Creator findById(String id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND,
                        "Creator không tồn tại với id: " + id));
    }

    private Creator findCreatorByAccountId(String id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND,
                        "Creator không tồn tại với account id: " + id));
    }

    private Sort getSort(CreatorFilterRequestDto filterRequest) {
        String sortBy = filterRequest.getSortBy();
        String sortDirection = filterRequest.getSortDirection();

        if (ValidationUtils.isNullOrEmpty(sortBy)) {
            if (!ValidationUtils.isNullOrEmpty(filterRequest.getSearchKey())) {
                return Sort.unsorted();
            }
            return Sort.by(parseSortDirection(sortDirection), "createdAt");
        }

        return Sort.by(parseSortDirection(sortDirection), normalizeSortProperty(sortBy));
    }

    private Sort.Direction parseSortDirection(String sortDirection) {
        if (sortDirection != null && sortDirection.equalsIgnoreCase("ASC")) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;
    }

    private String normalizeSortProperty(String sortBy) {
        if (ValidationUtils.isNullOrEmpty(sortBy)) {
            return "createdAt";
        }

        return switch (sortBy) {
            case "nickname", "bio", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }
}
