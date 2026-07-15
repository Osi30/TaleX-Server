package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorRegisterDto;
import com.talex.server.dtos.requests.filters.CreatorFilterRequestDto;
import com.talex.server.dtos.requests.terms.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.dtos.responses.creator.TermsVersionResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorTier;
import com.talex.server.enums.AccountStatus;
import com.talex.server.enums.TermsType;
import com.talex.server.exceptions.codes.CreatorErrorCode;
import com.talex.server.exceptions.details.CreatorException;
import com.talex.server.mappers.creator.ICreatorMapper;
import com.talex.server.records.CreatorVerificationStatus;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.services.creator.ICreatorIdentityService;
import com.talex.server.services.creator.ICreatorService;
import com.talex.server.services.creator.ICreatorTierService;
import com.talex.server.services.ekyc.IKycSessionService;
import com.talex.server.services.terms.ITermsLogService;
import com.talex.server.services.terms.ITermsVersionService;
import com.talex.server.specifications.CreatorSpec;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
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
    private final ICreatorIdentityService creatorIdentityService;
    private final IKycSessionService kycSessionService;
    private final CreatorRepository creatorRepository;
    private final AccountRepository accountRepository;
    private final ICreatorMapper creatorMapper;

    private final KafkaTemplate<String, String> kafkaTemplate;

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


    @Override
    @Transactional
    public String verifyCreator(CreatorRegisterDto dto) {
        Creator creator = getEntityByAccountId(dto.getAccountId());

        if (ValidationUtils.isNullOrEmpty(dto.getTermsId())) {
            throw new CreatorException(CreatorErrorCode.INVALID_CREATOR_REQUEST);
        }

        // Chưa đồng ý điều khoản
//        if (!creatorTermsLogService.existsByAccountAndTerm(dto.getAccountId(), dto.getTermsId())) {
        // Log
        creatorTermsLogService.create(
                dto.getAccountId(),
                CreatorTermsLogRequestDto.builder()
                        .versionId(dto.getTermsId())
                        .build());
        // Identity
        creatorIdentityService.create(creator);
//        }

        // Session
//        return kycSessionService.createSession(creator);
        creator.setIsVerified(true);
        creatorRepository.save(creator);
        return "Xác thực thành công";
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorVerificationStatus checkAndGetVerificationStatus(UUID accountId) {
        CreatorVerificationStatus status = creatorRepository.getVerificationStatusByAccountId(accountId)
                .orElseThrow(() -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND,
                        "Không tìm thấy hồ sơ Creator liên kết với tài khoản này."));

        // Kiểm tra xem Creator đã được verify (eKYC) chưa
        if (!Boolean.TRUE.equals(status.isCreatorVerified())) {
            throw new CreatorException(CreatorErrorCode.CREATOR_NOT_VERIFIED,
                    "Tài khoản này chưa hoàn tất các bước xác minh nhà sáng tạo.");
        }

        return status;
    }

    @Override
    @Async("requestAccountExecutor")
    public void sendUpdateRoleRequest(UUID accountId) {
        if (accountId == null) return;
        kafkaTemplate.send("request-to-update-account", accountId.toString());
    }

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
        TermsVersionResponseDto activeTerm = termsVersionService.getActiveByType(TermsType.CREATOR);

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

    @Cacheable(
            value = "accountToCreator",
            key = "#accountId",
            unless = "#result == null",
            cacheManager = "redisCacheManager"
    )
    @Override
    @Transactional(readOnly = true)
    public String getIdByAccountId(UUID accountId) {
        return creatorRepository.findCreatorIdByAccountId(accountId)
                .orElseThrow(() -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND, "Không tìm thấy hồ sơ Creator cho tài khoản."));
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
