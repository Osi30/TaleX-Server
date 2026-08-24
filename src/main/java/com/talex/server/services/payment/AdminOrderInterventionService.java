package com.talex.server.services.payment;

import com.talex.server.entities.transaction.Order;

import java.util.UUID;

public interface AdminOrderInterventionService {

    /**
     * Admin hủy đơn đang AWAITING_PAYMENT hoặc OUT_OF_TIME, hoàn Coin/Campaign Wallet đã áp (nếu có).
     */
    Order cancelByAdmin(String orderId, UUID adminId, String reason);

    /**
     * Admin đánh dấu đơn hoàn tất thủ công (tiền đã về nhưng webhook SePay không chạy được).
     * Tái dùng đúng đường {@code OrderCompletionService.complete()}/{@code completeViaWalletOnly()}
     * mà webhook lẽ ra đã gọi.
     */
    Order forceCompleteByAdmin(String orderId, UUID adminId, String reason);
}
