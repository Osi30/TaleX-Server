package com.talex.server.dtos.subscription.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorPoolDetailResponseDto {

    @JsonProperty("account_subscription_id")
    private String accountSubscriptionId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("total_amount")
    private Double totalAmount;

    @JsonProperty("vat_amount")
    private Double vatAmount;

    @JsonProperty("fiat_amount")
    private Double fiatAmount;

    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @JsonProperty("end_time")
    private LocalDateTime endTime;

    @JsonProperty("total_views")
    private Long totalViews;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("email")
    private String email;
}