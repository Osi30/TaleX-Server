package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CreatorTierErrorCode {
    NOT_FOUND(4060, HttpStatus.NOT_FOUND, "Không tìm thấy cấp độ creator"),
    LEVEL_ALREADY_EXISTS(4061, HttpStatus.BAD_REQUEST, "Cấp độ (tier level) đã tồn tại"),
    INVALID_REQUEST(4062, HttpStatus.BAD_REQUEST, "Yêu cầu cấp độ creator không hợp lệ");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
