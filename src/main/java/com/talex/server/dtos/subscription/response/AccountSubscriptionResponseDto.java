package com.talex.server.dtos.subscription.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSubscriptionResponseDto {
    private String accountSubscriptionId;
    private String accountId;
    private String subscriptionId;
    private String orderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isCancelled;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;
    /** Null nếu subscription này được Admin/Staff cấp thủ công (không có Order/Invoice gốc). */
    private String invoiceUrl;
}
