package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.PaymentProfileRequestDto;
import com.talex.server.dtos.requests.creator.PaymentProfileVerifiedDto;
import com.talex.server.dtos.requests.filters.PaymentProfileFilterRequestDto;
import com.talex.server.dtos.responses.creator.PaymentProfileResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.PaymentProfile;
import com.talex.server.enums.creator.PaymentProfileStatus;
import com.talex.server.exceptions.codes.CreatorErrorCode;
import com.talex.server.exceptions.codes.PaymentProfileErrorCode;
import com.talex.server.exceptions.details.CreatorException;
import com.talex.server.exceptions.details.PaymentProfileException;
import com.talex.server.mappers.creator.IPaymentProfileMapper;
import com.talex.server.repositories.creator.PaymentProfileRepository;
import com.talex.server.services.creator.ICreatorService;
import com.talex.server.services.creator.IPaymentProfileService;
import com.talex.server.specifications.PaymentProfileSpec;
import com.talex.server.utils.PageUtils;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentProfileService implements IPaymentProfileService {
    private final PaymentProfileRepository repository;
    private final ICreatorService creatorService;
    private final IPaymentProfileMapper mapper;

    @Override
    @Transactional
    public PaymentProfileResponseDto create(UUID accountId, PaymentProfileRequestDto dto) {
        // Validate creator exists
        Creator creator = creatorService.getEntityByAccountId(accountId);
        if (!creator.getIsVerified()) throw new CreatorException(CreatorErrorCode.CREATOR_NOT_VERIFIED);

        PaymentProfile entity = mapper.toEntity(dto);
        entity.setCreator(creator);
        entity.setIsPrimary(creator.getPaymentProfiles().isEmpty());
        entity.setStatus(PaymentProfileStatus.PENDING);

        PaymentProfile saved = repository.save(entity);
        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentProfileResponseDto getById(String id) {

        return mapper.toResponseDto(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentProfileResponseDto getPrimaryProfile(UUID accountId) {
        return repository.findByCreator_Account_AccountIdAndIsPrimaryTrueAndIsDeletedFalse(accountId)
                .map(mapper::toResponseDto)
                .orElseThrow(() -> new PaymentProfileException(
                        PaymentProfileErrorCode.NOT_FOUND,
                        "Không tìm thấy hồ sơ thanh toán chính cho tài khoản: " + accountId
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentProfileResponseDto> getOwnProfiles(UUID accountId) {
        return repository.findByCreator_Account_AccountIdAndIsDeletedFalse(accountId)
                .stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public PaymentProfileResponseDto update(String id, PaymentProfileRequestDto dto) {
        PaymentProfile existing = findById(id);

        // If setting as primary, unset others
        if (Boolean.TRUE.equals(dto.getIsPrimary())
                && !existing.getIsPrimary()
                && existing.getStatus().equals(PaymentProfileStatus.VERIFIED)
        ) {
            repository.unsetOtherPrimary(existing.getCreator().getCreatorId(), id);
        }

        mapper.updateEntity(dto, existing);

        PaymentProfile saved = repository.save(existing);

        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public PaymentProfileResponseDto updateVerifiedStatus(String id, PaymentProfileVerifiedDto dto) {
        PaymentProfile existing = findById(id);

        if (dto.getStatus().equals(existing.getStatus())){
            throw new PaymentProfileException(PaymentProfileErrorCode.INVALID_STATUS, "Status update bị trùng");
        }

        if (!dto.getStatus().equals(PaymentProfileStatus.CANCELLED)){
            existing.setStatus(dto.getStatus());
        }
        existing.setVerifiedNote(dto.getVerifiedNote());
        existing.setVerifiedAt(LocalDateTime.now());

        PaymentProfile saved = repository.save(existing);

        if (existing.getStatus().equals(PaymentProfileStatus.VERIFIED)){
            creatorService.sendUpdateRoleRequest(existing.getCreator().getAccount().getAccountId());
        }

        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        PaymentProfile existing = findById(id);

        if (Boolean.TRUE.equals(existing.getIsPrimary()) && existing.getStatus().equals(PaymentProfileStatus.VERIFIED)) {
            throw new PaymentProfileException(
                    PaymentProfileErrorCode.PRIMARY_PROFILE_REQUIRED,
                    "Không thể xóa hồ sơ thanh toán chính duy nhất của creator, nếu có một hồ sơ khác thì hãy chuyển mặc định sang tài khoản ấy."
            );
        } else {
            existing.setIsDeleted(true);
            repository.save(existing);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<PaymentProfileResponseDto> list(PaymentProfileFilterRequestDto filterRequest) {
        Sort sort = buildSort(filterRequest);
        Pageable pageable = PageUtils.buildPageable(filterRequest.getPage(), filterRequest.getPageSize(), sort);

        Page<PaymentProfile> pageResult = repository.findAll(
                PaymentProfileSpec.filterByCriteria(filterRequest.getCriteria(), filterRequest.getCreatorId()),
                pageable
        );

        List<PaymentProfileResponseDto> content = pageResult.stream()
                .map(mapper::toResponseDto)
                .toList();

        return BasePageResponse.<PaymentProfileResponseDto>builder()
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
    public PaymentProfile findById(String id) {
        return repository.findByPaymentProfileIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new PaymentProfileException(
                        PaymentProfileErrorCode.NOT_FOUND,
                        "Không tìm thấy hồ sơ thanh toán với id: " + id
                ));
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
            case "bankCode", "accountName", "status", "isPrimary", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }
}
