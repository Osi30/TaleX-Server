package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EngagementErrorCode {
    NOT_FOUND(4040, HttpStatus.NOT_FOUND, "Không tìm thấy gói dịch vụ"),
    INVALID_REQUEST(4001, HttpStatus.BAD_REQUEST, "Yêu cầu gói dịch vụ không hợp lệ"),
    TYPE_INVALID(4002, HttpStatus.BAD_REQUEST, "Loại dịch vụ không hợp lệ"),
    TARGET_INVALID(4002, HttpStatus.BAD_REQUEST, "Loại mục tiêu không hợp lệ");


    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
