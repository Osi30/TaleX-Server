package com.talex.server.services.creator;

import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.entities.series.EpisodeUnlockedContent;
import com.talex.server.entities.transaction.Order;

import java.util.List;

public interface RevenueTransactionService {
    /**
     * Tạo giao dịch doanh thu cho Creator khi có đơn hàng mua nội dung (EPISODE / COMBO) thành công.
     *
     * @param order Đơn hàng
     * @param unlockedContents Danh sách các EpisodeUnlockedContent được tạo từ order
     * @return RevenueTransaction đã lưu
     */
    RevenueTransaction createFromEpisodeOrder(Order order, List<EpisodeUnlockedContent> unlockedContents);
}