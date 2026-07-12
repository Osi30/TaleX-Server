package com.talex.server.services.subscription;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.subscription.AccountSubscriptionRequestDto;
import com.talex.server.dtos.responses.subscription.AccountSubscriptionResponseDto;

import java.util.UUID;

public interface IAccountSubscriptionService {
    AccountSubscriptionResponseDto createAccountSubscription(AccountSubscriptionRequestDto requestDto);

    BasePageResponse<AccountSubscriptionResponseDto> filterAndSortAccountSubscriptions(BaseFilterRequestDto filterRequest);

    AccountSubscriptionResponseDto getAccountSubscriptionById(String accountSubscriptionId);

    /**
     * @param requesterId  the caller's accountId (from JWT)
     * @param isPrivileged true if caller has ADMIN/STAFF role — bypasses the ownership check
     */
    void cancelAccountSubscription(String accountSubscriptionId, UUID requesterId, boolean isPrivileged);
}
