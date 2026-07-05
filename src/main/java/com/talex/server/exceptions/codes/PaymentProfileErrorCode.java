package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentProfileErrorCode {
    NOT_FOUND(4070, HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ thanh toán"),
    PRIMARY_PROFILE_REQUIRED(4071, HttpStatus.BAD_REQUEST, "Phải có ít nhất một hồ sơ thanh toán chính"),
    DUPLICATE_PRIMARY(4072, HttpStatus.BAD_REQUEST, "Chỉ có thể có một hồ sơ thanh toán chính"),
    INVALID_STATUS(4073, HttpStatus.BAD_REQUEST, "Trạng thái hồ sơ thanh toán không hợp lệ"),
    CREATOR_NOT_FOUND(4074, HttpStatus.NOT_FOUND, "Không tìm thấy creator"),
    INVALID_REQUEST(4075, HttpStatus.BAD_REQUEST, "Yêu cầu hồ sơ thanh toán không hợp lệ");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
