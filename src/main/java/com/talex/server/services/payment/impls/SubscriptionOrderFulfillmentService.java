package com.talex.server.services.payment.impls;

import com.talex.server.dtos.requests.subscription.AccountSubscriptionRequestDto;
import com.talex.server.entities.transaction.Order;
import com.talex.server.services.payment.IOrderFulfillmentService;
import com.talex.server.services.subscription.IAccountSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionOrderFulfillmentService implements IOrderFulfillmentService {

    public static final String ITEM_TYPE = "SUBSCRIPTION";

    private final IAccountSubscriptionService accountSubscriptionService;

    @Override
    public String getSupportedItemType() {
        return ITEM_TYPE;
    }

    @Override
    public void fulfill(Order order) {
        AccountSubscriptionRequestDto subscriptionRequest = AccountSubscriptionRequestDto.builder()
                .accountId(order.getAccount().getAccountId())
                .subscriptionId(order.getItemId())
                .build();
        accountSubscriptionService.createAccountSubscription(subscriptionRequest);
    }
}
