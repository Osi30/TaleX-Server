package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SubscriptionErrorCode {
    SUBSCRIPTION_NOT_FOUND(4041, HttpStatus.NOT_FOUND, "Subscription không tồn tại"),
    SUBSCRIPTION_ACCOUNT_NOT_FOUND(4042, HttpStatus.NOT_FOUND, "Account Subscription không tồn tại"),
    SUBSCRIPTION_ACCOUNT_FORBIDDEN(4043, HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên gói đăng ký này"),
    SUBSCRIPTION_INVALID_SORT(4001, HttpStatus.BAD_REQUEST, "Thuộc tính sắp xếp không hợp lệ"),
    SUBSCRIPTION_ACCOUNT_INVALID_STATUS_UPDATED(4001, HttpStatus.BAD_REQUEST, "Trạng thái cập nhập không hợp lệ");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
