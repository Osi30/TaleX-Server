package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CreatorErrorCode {
    CREATOR_NOT_FOUND(4051, HttpStatus.NOT_FOUND, "Không tìm thấy creator"),
    CREATOR_ALREADY_EXISTS(4052, HttpStatus.BAD_REQUEST, "Creator đã tồn tại"),
    INVALID_CREATOR_REQUEST(4053, HttpStatus.BAD_REQUEST, "Dữ liệu creator không hợp lệ");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
