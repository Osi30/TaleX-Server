package com.talex.server.records;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderDetailStatisticProjection {
    String getOrderId();
    BigDecimal getTotalAmount();
    Long getCoinAmount();
    BigDecimal getVatAmount();
    BigDecimal getShareAmount();
    String getDescription();
    BigDecimal getFiatAmount();
    String getStatus();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    String getItemType();
    String getItemId();
    String getItemName();
    String getAccountId();
    String getEmail();
    String getFullName();
}