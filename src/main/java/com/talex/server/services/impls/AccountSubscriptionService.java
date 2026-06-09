package com.talex.server.services.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.AccountSubscriptionRequestDto;
import com.talex.server.dtos.requests.filters.AccountSubscriptionFilterRequestDto;
import com.talex.server.dtos.responses.AccountSubscriptionResponseDto;
import com.talex.server.entities.Account;
import com.talex.server.entities.AccountSubscription;
import com.talex.server.entities.Subscription;
import com.talex.server.enums.AccountSubscriptionStatus;
import com.talex.server.exceptions.codes.SubscriptionErrorCode;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.exceptions.details.SubscriptionException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.AccountSubscriptionRepository;
import com.talex.server.services.IAccountSubscriptionService;
import com.talex.server.services.ISubscriptionService;
import com.talex.server.specifications.AccountSubscriptionSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountSubscriptionService implements IAccountSubscriptionService {
    private final ISubscriptionService subscriptionService;
    private final AccountSubscriptionRepository accountSubscriptionRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public AccountSubscriptionResponseDto createAccountSubscription(AccountSubscriptionRequestDto requestDto) {
        Account account = fetchAccount(requestDto.getAccountId());
        Subscription subscription = subscriptionService.getSubscriptionByIdEntity(requestDto.getSubscriptionId());
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plus(
                subscription.getDuration(),
                ChronoUnit.valueOf(subscription.getDurationUnit().toUpperCase()));

        AccountSubscription entity = AccountSubscription.builder()
                .account(account)
                .subscription(subscription)
                .startTime(startTime)
                .endTime(endTime)
                .status(AccountSubscriptionStatus.ACTIVE)
                .isAdBlocked(subscription.getIsAdBlocked())
                .isMovieUnlocked(subscription.getIsMovieUnlocked())
                .isStoryUnlocked(subscription.getIsStoryUnlocked())
                .build();

        AccountSubscription saved = accountSubscriptionRepository.save(entity);
        return toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<AccountSubscriptionResponseDto> filterAndSortAccountSubscriptions(AccountSubscriptionFilterRequestDto filterRequest) {
        AccountSubscriptionStatus[] statuses = parseStatuses(filterRequest.getStatuses());
        Pageable pageable = buildPageable(filterRequest);

        Specification<AccountSubscription> specification =
                AccountSubscriptionSpec.filterByCriteria(filterRequest, statuses);
        Page<AccountSubscription> subscriptions = accountSubscriptionRepository.findAll(specification, pageable);

        List<AccountSubscriptionResponseDto> content = subscriptions.stream()
                .map(this::toResponseDto)
                .toList();

        return BasePageResponse.<AccountSubscriptionResponseDto>builder()
                .content(content)
                .pageNumber(subscriptions.getNumber() + 1)
                .pageSize(subscriptions.getSize())
                .totalElements(subscriptions.getTotalElements())
                .totalPages(subscriptions.getTotalPages())
                .isFirst(subscriptions.isFirst())
                .isLast(subscriptions.isLast())
                .build();
    }

    private Pageable buildPageable(AccountSubscriptionFilterRequestDto filterRequest) {
        int page = validatePage(filterRequest.getPage());
        int pageSize = validatePageSize(filterRequest.getPageSize());
        Sort sort = buildSort(filterRequest.getSortBy(), filterRequest.getSortDirection());

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
            return Sort.by(Sort.Direction.DESC, "startTime");
        }

        Sort.Direction direction = parseSortDirection(sortDirection);
        return switch (sortBy) {
            case "startTime", "endTime", "updatedAt" -> Sort.by(direction, sortBy);
            default -> Sort.by(direction, "startTime");
        };
    }

    private Sort.Direction parseSortDirection(String sortDirection) {
        if (sortDirection != null && sortDirection.equalsIgnoreCase("ASC")) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;
    }

    private AccountSubscriptionStatus[] parseStatuses(String[] statuses) {
        if (statuses == null || statuses.length == 0) {
            return new AccountSubscriptionStatus[0];
        }

        AccountSubscriptionStatus[] results = new AccountSubscriptionStatus[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            try {
                results[i] = AccountSubscriptionStatus.valueOf(statuses[i].toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ACCOUNT_INVALID_STATUS_UPDATED,
                        "Invalid AccountSubscription status: " + statuses[i]);
            }
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountSubscriptionResponseDto getAccountSubscriptionById(String accountSubscriptionId) {
        return toResponseDto(findById(accountSubscriptionId));
    }

    @Override
    @Transactional
    public AccountSubscriptionResponseDto updateAccountSubscription(
            String accountSubscriptionId,
            AccountSubscriptionRequestDto requestDto
    ) {
        AccountSubscription subscription = findById(accountSubscriptionId);
        if (subscription.getStatus().equals(AccountSubscriptionStatus.ACTIVE)
        ) {
            if (requestDto.getStatus() != null && !requestDto.getStatus().equals(AccountSubscriptionStatus.ACTIVE)) {
                subscription.setStatus(requestDto.getStatus());
                accountSubscriptionRepository.save(subscription);
            }
            return toResponseDto(subscription);
        }

        throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ACCOUNT_INVALID_STATUS_UPDATED);
    }

    @Override
    @Transactional
    public void deleteAccountSubscription(String accountSubscriptionId) {
        AccountSubscription subscription = findById(accountSubscriptionId);

        if (subscription.getStatus().equals(AccountSubscriptionStatus.ACTIVE)
        ) {
            subscription.setStatus(AccountSubscriptionStatus.CANCELLED);
            accountSubscriptionRepository.save(subscription);
        }

        throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ACCOUNT_INVALID_STATUS_UPDATED);
    }

    private AccountSubscription findById(String id) {
        return accountSubscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ACCOUNT_NOT_FOUND,
                        "AccountSubscription not found with id: " + id));
    }

    private Account fetchAccount(UUID accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account id is required");
        }

        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private AccountSubscriptionResponseDto toResponseDto(AccountSubscription subscription) {
        if (subscription == null) {
            return null;
        }

        return AccountSubscriptionResponseDto.builder()
                .accountSubscriptionId(subscription.getAccountSubscriptionId())
                .accountId(
                        subscription.getAccount() != null ? subscription.getAccount().getAccountId().toString() : null)
                .subscriptionId(
                        subscription.getSubscription() != null ? subscription.getSubscription().getSubscriptionId()
                                : null)
                .startTime(subscription.getStartTime())
                .endTime(subscription.getEndTime())
                .updatedAt(subscription.getUpdatedAt())
                .status(subscription.getStatus())
                .isAdBlocked(subscription.getIsAdBlocked())
                .isMovieUnlocked(subscription.getIsMovieUnlocked())
                .isStoryUnlocked(subscription.getIsStoryUnlocked())
                .build();
    }
}
