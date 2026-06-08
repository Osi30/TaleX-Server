package com.talex.server.services.impls;

import com.talex.server.dtos.requests.KycSessionRequestDto;
import com.talex.server.dtos.requests.filters.KycSessionFilterRequestDto;
import com.talex.server.dtos.responses.KycSessionPageResponseDto;
import com.talex.server.dtos.responses.KycSessionResponseDto;
import com.talex.server.entities.Creator;
import com.talex.server.entities.KycSession;
import com.talex.server.enums.KycStatus;
import com.talex.server.exceptions.codes.KycSessionErrorCode;
import com.talex.server.exceptions.details.KycSessionException;
import com.talex.server.mappers.IKycSessionMapper;
import com.talex.server.repositories.KycSessionRepository;
import com.talex.server.services.IKycSessionService;
import com.talex.server.specifications.KycSessionSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KycSessionService implements IKycSessionService {
    private final KycSessionRepository kycSessionRepository;
    private final IKycSessionMapper kycSessionMapper;

    @Override
    @Transactional
    public String createSession(Creator creator) {
        kycSessionRepository.bulkUpdateStatus(
                creator.getCreatorId(),
                KycStatus.IN_PROGRESS,
                KycStatus.OUT_OF_TIME
        );

        KycSession kycSession = KycSession.builder()
                .status(KycStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .creator(creator)
                .build();

        KycSession savedSession = kycSessionRepository.save(kycSession);
        return savedSession.getKycSessionId();
    }

    @Override
    @Transactional(readOnly = true)
    public KycSessionResponseDto getSessionById(String kycSessionId) {
        return kycSessionMapper.toResponseDto(getById(kycSessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public KycSessionPageResponseDto filterAndSortSessions(KycSessionFilterRequestDto filterRequest) {
        Map<String, Object> criteria = getCriteria(filterRequest);
        KycStatus[] kycStatuses = parseStatuses(filterRequest.getStatuses());
        Pageable pageable = buildPageable(filterRequest);

        return executeQuery(criteria, kycStatuses, pageable);
    }

    @Override
    public KycSessionResponseDto updateSession(String kycSessionId, KycSessionRequestDto requestDto) {
        KycSession kycSession = getById(kycSessionId);

        if (requestDto.getStatus() != null &&
                kycSession.getStatus().equals(KycStatus.IN_PROGRESS)) {
            kycSession.setStatus(requestDto.getStatus());
        }

        KycSession updatedSession = kycSessionRepository.save(kycSession);
        return kycSessionMapper.toResponseDto(updatedSession);
    }

    @Override
    public KycSession getById(String kycSessionId) {
        return kycSessionRepository.findById(kycSessionId)
                .orElseThrow(() -> new KycSessionException(
                        KycSessionErrorCode.KYC_SESSION_NOT_FOUND,
                        "KYC Session not found with id: " + kycSessionId)
                );
    }

    @Override
    public KycSession getInProgressSession(String kycSessionId) {
        KycSession kycSession = getById(kycSessionId);
        if (kycSession.getStatus() == KycStatus.IN_PROGRESS) {
            return kycSession;
        }

        throw new KycSessionException(KycSessionErrorCode.KYC_SESSION_NOT_FOUND, "KYC Session in progress not found with id: " + kycSessionId);
    }

    private Map<String, Object> getCriteria(KycSessionFilterRequestDto filterRequest) {
        return filterRequest.getCriteria() != null ? filterRequest.getCriteria() : new HashMap<>();
    }

    private KycStatus[] parseStatuses(String[] statuses) {
        if (statuses == null || statuses.length == 0) {
            return new KycStatus[0];
        }

        KycStatus[] kycStatuses = new KycStatus[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            try {
                kycStatuses[i] = KycStatus.valueOf(statuses[i].toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new KycSessionException(
                        KycSessionErrorCode.KYC_SESSION_STATUS_INVALID,
                        "Invalid KYC status: " + statuses[i]
                );
            }
        }
        return kycStatuses;
    }

    private Pageable buildPageable(KycSessionFilterRequestDto filterRequest) {
        int page = validatePage(filterRequest.getPage());
        int pageSize = validatePageSize(filterRequest.getPageSize());
        Sort sort = buildSort(filterRequest.getSortBy(), filterRequest.getSortDirection());

        // Convert 1-based page to 0-based for PageRequest
        int zeroBasedPage = page - 1;

        return PageRequest.of(zeroBasedPage, pageSize, sort);
    }

    private int validatePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int validatePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20;
        }
        return pageSize;
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "startedAt");
        }

        Sort.Direction direction = parseSortDirection(sortDirection);

        // Handle null values for completedAt when sorting
        if ("completedAt".equals(sortBy)) {
            return Sort.by(direction, sortBy)
                    .and(Sort.by(Sort.Direction.DESC, "startedAt"));
        }

        return Sort.by(direction, sortBy);
    }

    private Sort.Direction parseSortDirection(String sortDirection) {
        if (sortDirection != null && sortDirection.equalsIgnoreCase("ASC")) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;
    }

    private KycSessionPageResponseDto executeQuery(
            Map<String, Object> criteria,
            KycStatus[] kycStatuses,
            Pageable pageable) {

        Specification<KycSession> specification = KycSessionSpec.filterByCriteria(criteria, kycStatuses);
        Page<KycSession> sessionPage = kycSessionRepository.findAll(specification, pageable);
        Page<KycSessionResponseDto> responseDtoPage = sessionPage.map(kycSessionMapper::toResponseDto);

        return kycSessionMapper.toPageResponseDto(responseDtoPage);
    }
}
