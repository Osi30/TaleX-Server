package com.talex.server.services.payment;

import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.PaymentMethod;

import java.math.BigDecimal;

/**
 * Hoàn tất một Order: ghi Transaction thành công, chuyển status COMPLETED,
 * rồi cấp quyền nội dung/gói qua {@link IOrderFulfillmentService} tương ứng với itemType.
 * <p>
 * Dùng chung cho cả luồng webhook SePay và luồng thanh toán bằng Coin — đảm bảo
 * hai luồng không phân kỳ logic cấp quyền.
 * </p>
 */
public interface OrderCompletionService {
    void complete(Order order, BigDecimal paidAmount, PaymentMethod paymentMethod);
}
