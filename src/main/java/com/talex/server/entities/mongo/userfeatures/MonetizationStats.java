package com.talex.server.entities.mongo.userfeatures;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonetizationStats {
    @Field("total_spent_amount")
    private BigDecimal totalSpentAmount = BigDecimal.ZERO;
    @Field("premium_subscription_count")
    private Long premiumSubscriptionCount = 0L;
    @Field("single_purchase_count")
    private Long singlePurchaseCount = 0L;
    @Field("interaction_push_count")
    private Long interactionPushCount = 0L;
    // -1 đại diện cho chưa phát sinh giao dịch
    @Field("days_since_last_purchase")
    private LocalDateTime lastPurchaseTime;
}
