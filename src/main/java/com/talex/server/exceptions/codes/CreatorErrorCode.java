package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CreatorErrorCode {
    CREATOR_NOT_FOUND(4041, HttpStatus.NOT_FOUND, "Không tìm thấy creator"),
    CREATOR_ALREADY_EXISTS(4001, HttpStatus.BAD_REQUEST, "Creator đã tồn tại"),
    INVALID_CREATOR_REQUEST(4002, HttpStatus.BAD_REQUEST, "Dữ liệu creator không hợp lệ"),

    CREATOR_NOT_VERIFIED(4003, HttpStatus.FORBIDDEN, "Tài khoản Creator chưa được hệ thống xác minh"),
    TERMS_NOT_ACCEPTED(4004, HttpStatus.BAD_REQUEST, "Creator chưa đồng ý với điều khoản bật kiếm tiền hiện hành"),
    IDENTITY_NOT_VERIFIED(4005, HttpStatus.BAD_REQUEST, "Thông tin định danh cá nhân (KYC) chưa hoàn tất xác thực"),
    PAYMENT_PROFILE_INVALID(4006, HttpStatus.BAD_REQUEST, "Tài khoản thanh toán chính chưa được cấu hình hoặc chưa xác thực thành công")    ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
