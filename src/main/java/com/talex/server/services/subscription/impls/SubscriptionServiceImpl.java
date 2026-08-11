package com.talex.server.services.subscription.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.subscription.request.SubscriptionRequestDto;
import com.talex.server.dtos.requests.filters.SubscriptionFilterRequestDto;
import com.talex.server.dtos.subscription.response.CreatorPoolSummaryResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionResponseDto;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.exceptions.codes.SubscriptionErrorCode;
import com.talex.server.exceptions.details.SubscriptionException;
import com.talex.server.mappers.subscription.SubscriptionMapper;
import com.talex.server.records.CreatorPoolData;
import com.talex.server.repositories.subscription.AccountSubscriptionRepository;
import com.talex.server.repositories.subscription.SubscriptionRepository;
import com.talex.server.services.subscription.SubscriptionService;
import com.talex.server.specifications.subscription.SubscriptionSpec;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final AccountSubscriptionRepository accountSubscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    @Transactional
    public SubscriptionResponseDto createSubscription(SubscriptionRequestDto requestDto) {
        Subscription subscription = subscriptionMapper.toEntity(requestDto);
        Subscription saved = subscriptionRepository.save(subscription);
        return subscriptionMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<SubscriptionResponseDto> filterSubscriptions(SubscriptionFilterRequestDto filterRequest) {
        if (filterRequest == null) {
            filterRequest = new SubscriptionFilterRequestDto();
        }

        int page = Optional.ofNullable(filterRequest.getPage()).orElse(1);
        int pageSize = Optional.ofNullable(filterRequest.getPageSize()).orElse(20);
        Sort sort = getSort(filterRequest);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        Page<Subscription> subscriptionPage = subscriptionRepository.findAll(
                Specification.where(SubscriptionSpec.filterByCriteria(filterRequest)),
                pageable);

        List<SubscriptionResponseDto> content = subscriptionPage.stream()
                .map(subscriptionMapper::toResponseDto)
                .toList();

        return BasePageResponse.<SubscriptionResponseDto>builder()
                .content(content)
                .pageNumber(subscriptionPage.getNumber() + 1)
                .pageSize(subscriptionPage.getSize())
                .totalElements(subscriptionPage.getTotalElements())
                .totalPages(subscriptionPage.getTotalPages())
                .isFirst(subscriptionPage.isFirst())
                .isLast(subscriptionPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponseDto getSubscriptionById(String subscriptionId) {
        return subscriptionMapper.toResponseDto(getSubscriptionByIdEntity(subscriptionId));
    }

    @Override
    @Transactional
    public SubscriptionResponseDto updateSubscription(String subscriptionId, SubscriptionRequestDto requestDto) {
        Subscription subscription = getSubscriptionByIdEntity(subscriptionId);
        subscriptionMapper.updateEntity(requestDto, subscription);

        if (subscription.getAccountSubscriptions().isEmpty()){
            Optional.ofNullable(requestDto.getPrice()).ifPresent(subscription::setPrice);
            Optional.ofNullable(requestDto.getDuration()).ifPresent(subscription::setDuration);
            Optional.ofNullable(requestDto.getDurationUnit())
                    .map(Enum::toString)
                    .ifPresent(subscription::setDurationUnit);
        }

        Subscription updated = subscriptionRepository.save(subscription);
        return subscriptionMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteSubscription(String subscriptionId) {
        Subscription subscription = getSubscriptionByIdEntity(subscriptionId);
        subscription.setIsDeleted(true);
        subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public Subscription getSubscriptionByIdEntity(String subscriptionId) {
        return subscriptionRepository.findBySubscriptionIdAndIsDeletedFalse(subscriptionId)
                .orElseThrow(() -> new SubscriptionException(
                        SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND,
                        "Subscription not found with id: " + subscriptionId));
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorPoolSummaryResponseDto getCreatorPoolSummary(int year, int month, String subscriptionId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);

        CreatorPoolData projection = accountSubscriptionRepository
                .calculateCreatorPoolSummary(startOfMonth, endOfMonth, subscriptionId);

        BigDecimal totalAmount = projection.totalAmount() != null ? projection.totalAmount() : BigDecimal.ZERO;
        BigDecimal taxAmount = projection.taxAmount() != null ? projection.taxAmount() : BigDecimal.ZERO;
        BigDecimal fiatAmount = projection.fiatAmount() != null ? projection.fiatAmount() : BigDecimal.ZERO;

        return CreatorPoolSummaryResponseDto.builder()
                .year(year)
                .month(month)
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .fiatAmount(fiatAmount)
                .totalSubscriptions(projection.totalSubscriptions() != null ? projection.totalSubscriptions() : 0L)
                .build();
    }

    private Sort getSort(SubscriptionFilterRequestDto filterRequest) {
        if (ValidationUtils.isNullOrEmpty(filterRequest.getSortBy())) {
            if (!ValidationUtils.isNullOrEmpty(
                    (String) filterRequest.getCriteria().get("searchKey"))
            ) {
                return Sort.unsorted();
            }
            return Sort.by(parseSortDirection(filterRequest.getSortDirection()), "createdAt");
        }

        return Sort.by(parseSortDirection(filterRequest.getSortDirection()),
                normalizeSortProperty(filterRequest.getSortBy()));
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
            case "price", "duration", "totalPurchases", "createdAt", "updatedAt" -> sortBy;
            default -> "createdAt";
        };
    }
}
