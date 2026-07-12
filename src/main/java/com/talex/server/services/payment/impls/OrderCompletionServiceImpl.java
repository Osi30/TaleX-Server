package com.talex.server.services.payment.impls;

import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.payment.IOrderFulfillmentService;
import com.talex.server.services.payment.ITransactionService;
import com.talex.server.services.payment.OrderCompletionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCompletionServiceImpl implements OrderCompletionService {

    private final OrderRepository orderRepository;
    private final ITransactionService transactionService;
    private final List<IOrderFulfillmentService> fulfillmentServices;

    private Map<String, IOrderFulfillmentService> fulfillmentServiceByItemType;

    @PostConstruct
    void indexFulfillmentServices() {
        fulfillmentServiceByItemType = fulfillmentServices.stream()
                .collect(Collectors.toMap(IOrderFulfillmentService::getSupportedItemType, Function.identity()));
    }

    @Override
    @Transactional
    public void complete(Order order, BigDecimal paidAmount, PaymentMethod paymentMethod) {
        transactionService.createSuccessTransaction(order, paidAmount, paymentMethod);

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        IOrderFulfillmentService fulfillmentService = fulfillmentServiceByItemType.get(order.getItemType());
        if (fulfillmentService == null) {
            throw new IllegalStateException(
                    "No IOrderFulfillmentService registered for itemType=" + order.getItemType());
        }
        fulfillmentService.fulfill(order);

        log.info("Order {} completed via {}, itemType={} itemId={} fulfilled",
                order.getOrderId(), paymentMethod, order.getItemType(), order.getItemId());
    }
}
