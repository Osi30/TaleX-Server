package com.talex.server.services;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.AccountSubscriptionRequestDto;
import com.talex.server.dtos.requests.filters.AccountSubscriptionFilterRequestDto;
import com.talex.server.dtos.responses.AccountSubscriptionResponseDto;

public interface IAccountSubscriptionService {
    AccountSubscriptionResponseDto createAccountSubscription(AccountSubscriptionRequestDto requestDto);

    BasePageResponse<AccountSubscriptionResponseDto> filterAndSortAccountSubscriptions(AccountSubscriptionFilterRequestDto filterRequest);

    AccountSubscriptionResponseDto getAccountSubscriptionById(String accountSubscriptionId);

    AccountSubscriptionResponseDto updateAccountSubscription(String accountSubscriptionId,
            AccountSubscriptionRequestDto requestDto);

    void deleteAccountSubscription(String accountSubscriptionId);
}
