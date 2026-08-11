package com.talex.server.services.creator;

import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.entities.transaction.Order;

public interface RevenueTransactionService {
    /**
     * Tạo giao dịch doanh thu cho Creator khi có đơn hàng mua tập phim thành công.
     *
     * @param order Đơn hàng mua tập phim (EPISODE)
     * @return RevenueTransaction đã được tạo và lưu
     */
    RevenueTransaction createFromEpisodeOrder(Order order);
}