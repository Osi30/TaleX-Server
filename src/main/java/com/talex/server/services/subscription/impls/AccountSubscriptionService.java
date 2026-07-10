package com.talex.server.services.subscription.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.subscription.AccountSubscriptionRequestDto;
import com.talex.server.dtos.responses.subscription.AccountSubscriptionResponseDto;
import com.talex.server.entities.Account;
import com.talex.server.entities.subscription.AccountSubscription;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.exceptions.codes.SubscriptionErrorCode;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.exceptions.details.SubscriptionException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.subscription.AccountSubscriptionRepository;
import com.talex.server.services.subscription.IAccountSubscriptionService;
import com.talex.server.services.subscription.ISubscriptionService;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        LocalDateTime startTime = calculateStartTime(account);
        LocalDateTime endTime = calculateEndTime(startTime, subscription);

        AccountSubscription entity = AccountSubscription.builder()
                .account(account)
                .subscription(subscription)
                .startTime(startTime)
                .endTime(endTime)
                .isAdBlocked(subscription.getIsAdBlocked())
                .isMovieUnlocked(subscription.getIsMovieUnlocked())
                .isStoryUnlocked(subscription.getIsStoryUnlocked())
                .build();

        AccountSubscription saved = accountSubscriptionRepository.save(entity);
        return toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<AccountSubscriptionResponseDto> filterAndSortAccountSubscriptions(BaseFilterRequestDto filterRequest) {
        Pageable pageable = buildPageable(filterRequest);

        Specification<AccountSubscription> specification =
                AccountSubscriptionSpec.filterByCriteria(filterRequest);
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

    @Override
    @Transactional(readOnly = true)
    public AccountSubscriptionResponseDto getAccountSubscriptionById(String accountSubscriptionId) {
        return toResponseDto(findById(accountSubscriptionId));
    }

    @Override
    @Transactional
    public void cancelAccountSubscription(String accountSubscriptionId, UUID requesterId, boolean isPrivileged) {
        AccountSubscription subscription = findById(accountSubscriptionId);
        if (!isPrivileged && !subscription.getAccount().getAccountId().equals(requesterId)) {
            throw new SubscriptionException(SubscriptionErrorCode.SUBSCRIPTION_ACCOUNT_FORBIDDEN);
        }
        subscription.setIsCancelled(true);
        subscription.setCancelledAt(LocalDateTime.now());
        accountSubscriptionRepository.save(subscription);
    }

    private Pageable buildPageable(BaseFilterRequestDto filterRequest) {
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

    private LocalDateTime calculateStartTime(Account account){
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime startTime = currentTime;
        Optional<AccountSubscription> latestValidSub = getLatestValidSub(account, currentTime);
        if (latestValidSub.isPresent()) {
            startTime = latestValidSub.get().getEndTime().plusSeconds(1);
        }

        return startTime;
    }

    private LocalDateTime calculateEndTime(LocalDateTime startTime, Subscription subscription) {
        return startTime.plus(
                subscription.getDuration(),
                ChronoUnit.valueOf(subscription.getDurationUnit().toUpperCase()));
    }

    private Optional<AccountSubscription> getLatestValidSub(Account account, LocalDateTime currentTime) {
        return account
                .getAccountSubscriptions().stream()
                .filter(sub -> currentTime.isBefore(sub.getEndTime()) && !sub.getIsCancelled())
                .max(Comparator.comparing(AccountSubscription::getEndTime));
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
                .cancelledAt(subscription.getCancelledAt())
                .isAdBlocked(subscription.getIsAdBlocked())
                .isMovieUnlocked(subscription.getIsMovieUnlocked())
                .isStoryUnlocked(subscription.getIsStoryUnlocked())
                .isCancelled(subscription.getIsCancelled())
                .build();
    }
}
