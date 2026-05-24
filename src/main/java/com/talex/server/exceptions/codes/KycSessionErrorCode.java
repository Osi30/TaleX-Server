package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum KycSessionErrorCode {
    KYC_SESSION_NOT_FOUND(4041, HttpStatus.NOT_FOUND, "Không tìm thấy phiên KYC tương ứng"),
    KYC_SESSION_STATUS_INVALID(4001, HttpStatus.BAD_REQUEST, "Trạng thái phiên không hợp lệ");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
