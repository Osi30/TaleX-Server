package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TermsVersionErrorCode {
    NOT_FOUND(4040, HttpStatus.NOT_FOUND, "Không tìm thấy phiên bản điều khoản"),
    ACTIVE_VERSION_NOT_FOUND(4041, HttpStatus.NOT_FOUND, "Không tìm thấy bản điều khoản đang kích hoạt"),
    INVALID_REQUEST(4001, HttpStatus.BAD_REQUEST, "Yêu cầu điều khoản không hợp lệ"),
    TERMS_TYPE_INVALID(4002, HttpStatus.BAD_REQUEST, "Loại terms không hợp lệ");


    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
