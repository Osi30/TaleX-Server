package com.talex.server.services.subscription;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.subscription.request.AccountSubscriptionRequestDto;
import com.talex.server.dtos.subscription.response.AccountSubscriptionResponseDto;
import com.talex.server.dtos.subscription.response.CreatorPoolDetailResponseDto;
import com.talex.server.dtos.subscription.response.MonthlyAccountSubscriptionResponseDto;

import java.util.UUID;

public interface AccountSubscriptionService {
    AccountSubscriptionResponseDto createAccountSubscription(AccountSubscriptionRequestDto requestDto);

    BasePageResponse<AccountSubscriptionResponseDto> filterAndSortAccountSubscriptions(BaseFilterRequestDto filterRequest);

    BasePageResponse<CreatorPoolDetailResponseDto> getCreatorPoolDetails(
            int year, int month, String subscriptionId, int page, int pageSize);

    BasePageResponse<MonthlyAccountSubscriptionResponseDto> getMonthlyAccountSubscriptions(
            int year, int month, int page, int pageSize);

    AccountSubscriptionResponseDto getAccountSubscriptionById(String accountSubscriptionId);

    /**
     * @param requesterId  the caller's accountId (from JWT)
     * @param isPrivileged true if caller has ADMIN/STAFF role — bypasses the ownership check
     */
    void cancelAccountSubscription(String accountSubscriptionId, UUID requesterId, boolean isPrivileged);

    AccountSubscriptionResponseDto getActiveAccountSubscription(UUID accountId);
}
