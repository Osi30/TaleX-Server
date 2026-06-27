package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorRegisterDto;
import com.talex.server.dtos.requests.creator.CreatorRequestDto;
import com.talex.server.dtos.requests.filters.CreatorFilterRequestDto;
import com.talex.server.dtos.requests.terms.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.dtos.responses.TermsVersionResponseDto;
import com.talex.server.entities.Account;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorTier;
import com.talex.server.enums.AccountStatus;
import com.talex.server.enums.TermsType;
import com.talex.server.exceptions.codes.CreatorErrorCode;
import com.talex.server.exceptions.details.CreatorException;
import com.talex.server.mappers.ICreatorMapper;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.services.creator.ICreatorService;
import com.talex.server.services.creator.ICreatorTierService;
import com.talex.server.services.terms.ITermsLogService;
import com.talex.server.services.terms.ITermsVersionService;
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
    private final ITermsVersionService termsVersionService;
    private final ITermsLogService creatorTermsLogService;
    private final ICreatorTierService creatorTierService;
    private final CreatorRepository creatorRepository;
    private final AccountRepository accountRepository;
    private final ICreatorMapper creatorMapper;

    @Override
    @Transactional
    public CreatorResponseDto createCreator(CreatorRegisterDto dto) {
        // 1. Creator
        Account account = accountRepository.findByAccountIdAndStatus(
                        dto.getAccountId(), AccountStatus.ACTIVE)
                .orElseThrow(() -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND, "No active account found with id: " + dto.getAccountId()));

        CreatorTier tier = creatorTierService.getDefaultTier();
        Creator creator = creatorRepository.save(Creator.builder()
                .creatorTier(tier)
                .account(account)
                .build());

        // 2. Log
        creatorTermsLogService.create(
                account,
                CreatorTermsLogRequestDto.builder()
                        .versionId(dto.getTermsId())
                        .build());

        return creatorMapper.toResponseDto(creator);
    }


//    @Override
//    @Transactional
//    public String createCreator(CreatorRegisterDto dto) {
//        Creator creator;
//
//        if (ValidationUtils.isNullOrEmpty(dto.getTermsId())) {
//            throw new CreatorException(CreatorErrorCode.INVALID_CREATOR_REQUEST);
//        }
//
//        // Đã đồng ý điều khoản
//        if (creatorTermsLogService.existsByAccountAndTerm(dto.getAccountId(), dto.getTermsId())) {
//            creator = findCreatorByAccountId(dto.getAccountId());
//        }
//        // Chưa đồng ý điều khoản
//        else {
//            // 1. Creator
//            Account account = accountRepository.findById(dto.getAccountId()).orElseThrow(
//                    () -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND));
//            creator = creatorRepository.save(Creator.builder()
//                    .account(account)
//                    .build());
//
//            // 2. Log
//            creatorTermsLogService.create(
//                    dto.getAccountId(),
//                    CreatorTermsLogRequestDto.builder()
//                            .versionId(dto.getTermsId())
//                            .build());
//
//            // 3. Identity
//            creatorIdentityService.create(creator);
//        }
//
//        // 4. Session
//        return kycSessionService.createSession(creator);
//    }

    @Override
    @Transactional(readOnly = true)
    public CreatorResponseDto getById(String id) {
        Creator creator = getEntityById(id);
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
    @Transactional(readOnly = true)
    public CreatorResponseDto getByAccount(UUID accountId) {
        // 1. Kiểm tra xem tài khoản này đã từng đăng ký Creator chưa
        Creator creator = getEntityByAccountId(accountId);

        // 2. Lấy bản điều khoản hiện hành loại CREATOR
        TermsVersionResponseDto activeTerm;
        try {
            Object raw = termsVersionService.getActiveByType(TermsType.CREATOR);
            if (raw instanceof TermsVersionResponseDto dto) {
                activeTerm = dto;
            } else {
                // Redis cache returns LinkedHashMap — convert via ObjectMapper
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                om.findAndRegisterModules();
                activeTerm = om.convertValue(raw, TermsVersionResponseDto.class);
            }
        } catch (Exception e) {
            activeTerm = null;
        }

        // 3. Kiểm tra đã đồng ý với term hiện hành chưa
        boolean hasAcceptedLatest = activeTerm != null && creatorTermsLogService.existsByAccountAndTerm(accountId, activeTerm.getId());

        // 4. Nếu vượt qua mọi tầng kiểm tra -> Trả dữ liệu Creator hợp lệ thông thường
        CreatorResponseDto responseDto = creatorMapper.toResponseDto(creator);
        responseDto.setIsAcceptedLatestTerms(hasAcceptedLatest);
        if (!hasAcceptedLatest) {
            responseDto.setTermsVersion(activeTerm);
        }

        return responseDto;
    }

    @Override
    public CreatorResponseDto updateCreator(String id, CreatorRequestDto dto) {
        Creator existing = getEntityById(id);
        Creator saved = creatorRepository.save(existing);
        return creatorMapper.toResponseDto(saved);
    }

    @Override
    public void deleteCreator(String id) {
        Creator existing = getEntityById(id);
        creatorRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Creator getEntityById(String id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND,
                        "Creator không tồn tại với id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Creator getEntityByAccountId(UUID id) {
        return creatorRepository.findByAccount_AccountId(id)
                .orElseThrow(() -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND,
                        "Tài khoản này hiện tại chưa đăng ký thông tin nhà sáng tạo."));
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
