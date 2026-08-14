package com.talex.server.services.media.impls;

import com.talex.server.dtos.requests.media.ViolationLabelTranslationCreateRequestDto;
import com.talex.server.dtos.requests.media.ViolationLabelTranslationUpdateRequestDto;
import com.talex.server.dtos.responses.media.ViolationLabelTranslationResponseDto;
import com.talex.server.entities.media.ViolationLabelTranslation;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.mappers.media.ViolationLabelTranslationMapper;
import com.talex.server.repositories.media.ViolationLabelTranslationRepository;
import com.talex.server.services.media.ViolationLabelTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationLabelTranslationServiceImpl implements ViolationLabelTranslationService {

    private final ViolationLabelTranslationRepository repository;
    private final ViolationLabelTranslationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ViolationLabelTranslationResponseDto> list() {
        return repository.findAllByIsDeletedFalseOrderByCategoryAscAwsLabelAsc().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ViolationLabelTranslationResponseDto create(UUID accountId, ViolationLabelTranslationCreateRequestDto request) {
        String awsLabel = request.getAwsLabel().trim();
        if (repository.existsByAwsLabelAndIsDeletedFalse(awsLabel)) {
            throw ContentModuleException.conflict("Nhãn AWS này đã có bản dịch: " + awsLabel);
        }

        ViolationLabelTranslation entity = ViolationLabelTranslation.builder()
                .awsLabel(awsLabel)
                .vietnameseText(request.getVietnameseText().trim())
                .category(request.getCategory() != null ? request.getCategory().trim() : null)
                .build();
        entity.markCreatedBy(accountId.toString());

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public ViolationLabelTranslationResponseDto update(UUID accountId, UUID translationId, ViolationLabelTranslationUpdateRequestDto request) {
        ViolationLabelTranslation entity = findManageableEntity(translationId);

        entity.setVietnameseText(request.getVietnameseText().trim());
        entity.setCategory(request.getCategory() != null ? request.getCategory().trim() : null);
        entity.markUpdatedBy(accountId.toString());

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID accountId, UUID translationId) {
        ViolationLabelTranslation entity = findManageableEntity(translationId);
        entity.softDelete(accountId.toString());
        repository.save(entity);
    }

    private ViolationLabelTranslation findManageableEntity(UUID translationId) {
        return repository.findByTranslationIdAndIsDeletedFalse(translationId)
                .orElseThrow(() -> ContentModuleException.notFound("Không tìm thấy bản dịch: " + translationId));
    }
}
