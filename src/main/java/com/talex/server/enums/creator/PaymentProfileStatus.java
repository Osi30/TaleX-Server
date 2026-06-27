package com.talex.server.enums.creator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentProfileStatus {
    PENDING("Đang chờ duyệt"),
    VERIFIED("Đã được duyệt"),
    REJECTED("Bị từ chối"),
    CANCELLED("Đã hủy");

    private final String detail;
}
