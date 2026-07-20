package com.talex.server.services.payment;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.responses.payment.OrderHistoryItemDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderHistoryService {
    BasePageResponse<OrderHistoryItemDto> getContentPurchaseHistory(UUID accountId, Pageable pageable);
}
