package com.talex.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KycStatus {
    IN_PROGRESS ("Đang trong quá trình đăng ký"),
    CANCELLED ("Bị hủy 1 chiều bởi người đăng ký"),
    SUCCESS ("Đăng ký thành công"),
    REJECTED ("Bị từ chối"),
    OUT_OF_TIME ("Hết phiên do quá thời gian đăng ký");

    private final String detail;
}