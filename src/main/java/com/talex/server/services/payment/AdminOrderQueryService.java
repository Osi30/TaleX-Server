package com.talex.server.services.payment;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.responses.payment.AdminOrderDetailDto;
import com.talex.server.dtos.responses.payment.AdminOrderListItemDto;
import com.talex.server.dtos.responses.payment.AdminOrderStatsDto;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;

import java.time.LocalDateTime;

public interface AdminOrderQueryService {

    BasePageResponse<AdminOrderListItemDto> search(
            OrderStatus status, String itemType, LocalDateTime createdAtFrom, LocalDateTime createdAtTo,
            String keyword, int page, int pageSize);

    AdminOrderDetailDto getDetail(String orderId);

    BasePageResponse<AdminOrderListItemDto> listOverpaid(int page, int pageSize);

    AdminOrderStatsDto getStats(LocalDateTime from, LocalDateTime to);

    /** Map trực tiếp entity đã có sẵn (VD kết quả cancel/force-complete) — tránh fetch lại DB. */
    AdminOrderDetailDto toDetailDto(Order order);
}
