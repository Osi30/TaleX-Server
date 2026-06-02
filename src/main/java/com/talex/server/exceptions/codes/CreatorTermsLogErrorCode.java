package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CreatorTermsLogErrorCode {
    CREATOR_TERMS_LOG_NOT_FOUND(4056, HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi Creator Terms Log"),
    CREATOR_TERMS_LOG_INACTIVE_VERSION(4057, HttpStatus.BAD_REQUEST, "Bản điều khoản chưa được kích hoạt hoặc không hợp lệ"),
    INVALID_CREATOR_TERMS_LOG_REQUEST(4058, HttpStatus.BAD_REQUEST, "Yêu cầu Creator Terms Log không hợp lệ");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
