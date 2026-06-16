package com.talex.server.services.subscription;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.subscription.AccountSubscriptionRequestDto;
import com.talex.server.dtos.responses.subscription.AccountSubscriptionResponseDto;

public interface IAccountSubscriptionService {
    AccountSubscriptionResponseDto createAccountSubscription(AccountSubscriptionRequestDto requestDto);

    BasePageResponse<AccountSubscriptionResponseDto> filterAndSortAccountSubscriptions(BaseFilterRequestDto filterRequest);

    AccountSubscriptionResponseDto getAccountSubscriptionById(String accountSubscriptionId);

    void cancelAccountSubscription(String accountSubscriptionId);
}
