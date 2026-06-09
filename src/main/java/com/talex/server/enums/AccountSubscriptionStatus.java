package com.talex.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountSubscriptionStatus {
    ACTIVE("Gói đang hoạt động"),
    EXPIRED("Gói hết hạn sử dụng"),
    CANCELLED("Gói bị hủy từ người dùng");

    private final String detail;
}
