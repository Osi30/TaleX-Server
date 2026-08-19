package com.talex.server.services.subscription.impls;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.subscription.request.AccountSubscriptionRequestDto;
import com.talex.server.dtos.subscription.response.AccountSubscriptionResponseDto;
import com.talex.server.dtos.subscription.response.CreatorPoolDetailResponseDto;
import com.talex.server.dtos.subscription.response.MonthlyAccountSubscriptionResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.subscription.AccountSubscription;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.entities.transaction.Invoice;
import com.talex.server.entities.transaction.Transaction;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.exceptions.codes.SubscriptionErrorCode;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.exceptions.details.SubscriptionException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.subscription.AccountSubscriptionRepository;
import com.talex.server.repositories.transaction.InvoiceRepository;
import com.talex.server.repositories.transaction.TransactionRepository;
import com.talex.server.services.subscription.AccountSubscriptionService;
import com.talex.server.services.subscription.SubscriptionService;
import com.talex.server.services.payment.impls.AccountFulfillmentLock;
import com.talex.server.specifications.subscription.AccountSubscriptionSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountSubscriptionServiceImpl implements AccountSubscriptionService {
    private final SubscriptionService subscriptionService;
    private final AccountSubscriptionRepository accountSubscriptionRepository;
    private final AccountRepository accountRepository;
    private final AccountFulfillmentLock accountFulfillmentLock;
    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public AccountSubscriptionResponseDto createAccountSubscription(AccountSubscriptionRequestDto requestDto) {
        // Serialize việc tính startTime/tạo subscription của CÙNG account — tránh race
        // khi 2 order subscription (VD nâng cấp gói) hoàn tất gần như đồng thời: cả 2
        // đều đọc cùng "latest valid sub" cũ, tính startTime trùng nhau thay vì nối tiếp.
        accountFulfillmentLock.acquire(requestDto.getAccountId());

        Account account = fetchAccount(requestDto.getAccountId());
        Subscription subscription = subscriptionService.getSubscriptionByIdEntity(requestDto.getSubscriptionId());
        LocalDateTime startTime = calculateStartTime(account);
        LocalDateTime endTime = calculateEndTime(startTime, subscription);

        AccountSubscription entity = AccountSubscription.builder()
                .account(account)
                .subscription(subscription)
                .startTime(startTime)
                .endTime(endTime)
                .orderId(requestDto.getOrderId())
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

        Map<String, String> invoiceUrlByOrderId = resolveInvoiceUrls(subscriptions.getContent());
        List<AccountSubscriptionResponseDto> content = subscriptions.stream()
                .map(subscription -> toResponseDto(subscription, invoiceUrlByOrderId))
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
    public BasePageResponse<CreatorPoolDetailResponseDto> getCreatorPoolDetails(
            int year, int month, String subscriptionId, int page, int pageSize) {

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);

        int validPage = Math.max(page, 1);
        int validPageSize = pageSize < 1 ? 20 : pageSize;
        Pageable pageable = PageRequest.of(validPage - 1, validPageSize, Sort.by(Sort.Direction.DESC, "endTime"));

        Page<Object[]> pageResult = accountSubscriptionRepository.findCreatorPoolDetailsRaw(
                startOfMonth, endOfMonth, subscriptionId, pageable);

        List<CreatorPoolDetailResponseDto> content = pageResult.stream()
                .map(row -> {
                    Double totalAmount = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                    Double vatAmount = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                    Double fiatAmount = totalAmount - vatAmount;

                    return CreatorPoolDetailResponseDto.builder()
                            .accountSubscriptionId((String) row[0])
                            .orderId((String) row[1])
                            .totalAmount(totalAmount)
                            .vatAmount(vatAmount)
                            .fiatAmount(fiatAmount)
                            .startTime((LocalDateTime) row[4])
                            .endTime((LocalDateTime) row[5])
                            .totalViews(row[6] != null ? ((Number) row[6]).longValue() : 0L)
                            .accountId(Objects.toString(row[7], null))
                            .email((String) row[8])
                            .build();
                })
                .toList();

        return BasePageResponse.<CreatorPoolDetailResponseDto>builder()
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
    @Transactional(readOnly = true)
    public BasePageResponse<MonthlyAccountSubscriptionResponseDto> getMonthlyAccountSubscriptions(
            int year, int month, int page, int pageSize) {

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);

        int validPage = Math.max(page, 1);
        int validPageSize = pageSize < 1 ? 20 : pageSize;
        Pageable pageable = PageRequest.of(validPage - 1, validPageSize, Sort.by(Sort.Direction.DESC, "endTime"));

        Page<Object[]> pageResult = accountSubscriptionRepository.findMonthlyAccountSubscriptionsRaw(
                startOfMonth, endOfMonth, pageable);

        List<MonthlyAccountSubscriptionResponseDto> content = pageResult.stream()
                .map(row -> {
                    Double totalAmount = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                    Double vatAmount = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
                    Double amount = totalAmount - vatAmount; // Tính toán amount = total - vat

                    return MonthlyAccountSubscriptionResponseDto.builder()
                            .accountSubscriptionId((String) row[0])
                            .subscriptionId((String) row[1])
                            .orderId((String) row[2])
                            .totalAmount(totalAmount)
                            .vatAmount(vatAmount)
                            .amount(amount)
                            .startTime((LocalDateTime) row[5])
                            .endTime((LocalDateTime) row[6])
                            .totalViews(row[7] != null ? ((Number) row[7]).longValue() : 0L)
                            .accountId(Objects.toString(row[8], null))
                            .username((String) row[9])
                            .email((String) row[10])
                            .build();
                })
                .toList();

        return BasePageResponse.<MonthlyAccountSubscriptionResponseDto>builder()
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

    @Override
    @Transactional(readOnly = true)
    public AccountSubscriptionResponseDto getActiveAccountSubscription(UUID accountId) {
        return accountSubscriptionRepository
                .findActiveAccountSubId(
                        accountId, LocalDateTime.now())
                .map(this::toResponseDto)
                .orElse(null);
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

    // Order -> Transaction (ReferenceType.ORDER) -> Invoice, cùng pattern
    // OrderHistoryServiceImpl dùng cho lịch sử mua Combo/Episode — batch 1 lần thay vì
    // N+1 query mỗi item trong danh sách phân trang.
    private Map<String, String> resolveInvoiceUrls(List<AccountSubscription> subscriptions) {
        List<String> orderIds = subscriptions.stream()
                .map(AccountSubscription::getOrderId)
                .filter(orderId -> orderId != null && !orderId.isBlank())
                .toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Transaction> transactionByOrderId = transactionRepository
                .findByReferenceTypeAndReferenceIdIn(ReferenceType.ORDER, orderIds).stream()
                .collect(Collectors.toMap(Transaction::getReferenceId, Function.identity(), (a, b) -> a));

        List<String> transactionIds = transactionByOrderId.values().stream()
                .map(Transaction::getTransactionId).toList();
        Map<String, Invoice> invoiceByTransactionId = invoiceRepository
                .findByTransaction_TransactionIdIn(transactionIds).stream()
                .collect(Collectors.toMap(invoice -> invoice.getTransaction().getTransactionId(),
                        Function.identity(), (a, b) -> a));

        // Collectors.toMap() throw NullPointerException nếu value là null — Invoice có thể
        // đã được tạo (thanh toán thành công) nhưng invoiceUrl vẫn null vì hóa đơn điện tử
        // SePay xử lý bất đồng bộ, chưa kịp có URL ngay lúc vừa thanh toán xong. Lọc bỏ
        // luôn những entry chưa có invoiceUrl thay vì để value null lọt vào map.
        return transactionByOrderId.entrySet().stream()
                .filter(entry -> invoiceByTransactionId.containsKey(entry.getValue().getTransactionId()))
                .filter(entry -> invoiceByTransactionId.get(entry.getValue().getTransactionId()).getInvoiceUrl() != null)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> invoiceByTransactionId.get(entry.getValue().getTransactionId()).getInvoiceUrl()));
    }

    private AccountSubscriptionResponseDto toResponseDto(AccountSubscription subscription) {
        if (subscription == null) {
            return null;
        }
        return toResponseDto(subscription, resolveInvoiceUrls(List.of(subscription)));
    }

    private AccountSubscriptionResponseDto toResponseDto(
            AccountSubscription subscription, Map<String, String> invoiceUrlByOrderId) {
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
                .orderId(subscription.getOrderId())
                .startTime(subscription.getStartTime())
                .endTime(subscription.getEndTime())
                .updatedAt(subscription.getUpdatedAt())
                .cancelledAt(subscription.getCancelledAt())
                .isCancelled(subscription.getIsCancelled())
                .invoiceUrl(subscription.getOrderId() == null ? null : invoiceUrlByOrderId.get(subscription.getOrderId()))
                .build();
    }
}
