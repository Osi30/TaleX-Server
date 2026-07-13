package com.talex.server.services.payment.impls;

import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.repositories.transaction.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đánh dấu Order hết hạn trong 1 transaction RIÊNG (REQUIRES_NEW).
 * <p>
 * Bắt buộc phải tách riêng: {@code reuseOrBlockActiveOrder} ném {@link RuntimeException}
 * ngay sau khi set status OUT_OF_TIME — nếu chạy chung transaction với method gọi nó,
 * Spring sẽ rollback toàn bộ (kể cả status vừa set), khiến order mãi kẹt ở AWAITING_PAYMENT
 * và user bấm "Thử tạo lại đơn hàng" mãi mãi không được (luôn gặp lại đúng order cũ, đúng
 * lỗi cũ).
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrderExpirationMarker {

    private final OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExpired(Order order) {
        order.setStatus(OrderStatus.OUT_OF_TIME);
        orderRepository.save(order);
    }
}
