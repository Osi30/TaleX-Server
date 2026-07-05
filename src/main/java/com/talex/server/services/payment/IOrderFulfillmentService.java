package com.talex.server.services.payment;

import com.talex.server.entities.transaction.Order;

public interface IOrderFulfillmentService {
    String getSupportedItemType();

    void fulfill(Order order);
}
