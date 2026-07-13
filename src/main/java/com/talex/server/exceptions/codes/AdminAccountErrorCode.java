package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminAccountErrorCode {
    ACCOUNT_NOT_FOUND(4601, HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"),
    EMAIL_ALREADY_EXISTS(4602, HttpStatus.CONFLICT, "Email đã được sử dụng"),
    USERNAME_ALREADY_EXISTS(4603, HttpStatus.CONFLICT, "Username đã được sử dụng"),
    CANNOT_BAN_ADMIN(4604, HttpStatus.FORBIDDEN, "Không thể khóa tài khoản ADMIN"),
    INVALID_ACCOUNT_FILTER(4605, HttpStatus.BAD_REQUEST, "Bộ lọc tài khoản không hợp lệ");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
