package com.talex.server.dtos.requests.subscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSubscriptionRequestDto {
    @NotNull(message = "Account ID is required")
    private UUID accountId;
    private String subscriptionId;

    /** Order gốc khi subscription được cấp qua thanh toán — null nếu Admin/Staff cấp thủ công. */
    @JsonIgnore
    private String orderId;

    @JsonIgnore
    private LocalDateTime startTime;
    @JsonIgnore
    private LocalDateTime endTime;
}
