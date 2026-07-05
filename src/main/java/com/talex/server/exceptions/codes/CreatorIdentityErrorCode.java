package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CreatorIdentityErrorCode {
    CREATOR_IDENTITY_NOT_FOUND(4050, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin định danh creator"),
    CREATOR_IDENTITY_ALREADY_EXIST(4001, HttpStatus.BAD_REQUEST, "Định danh creator đã tồn tại"),
    INVALID_REQUEST(4003, HttpStatus.BAD_REQUEST, "Yêu cầu cho CreatorIdentity không hợp lệ"),
    INVALID_TAX_ID(4004, HttpStatus.BAD_REQUEST, "Mã số thuế không hợp lệ");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
