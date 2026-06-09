package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode {
    INVALID_CREDENTIALS(4010, HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    ACCOUNT_NOT_VERIFIED(4011, HttpStatus.UNAUTHORIZED, "Tài khoản chưa được xác minh"),
    ACCOUNT_BANNED(4012, HttpStatus.FORBIDDEN, "Tài khoản đã bị cấm"),
    ACCOUNT_DELETED(4013, HttpStatus.GONE, "Tài khoản đã bị xóa"),
    INVALID_VERIFICATION_TOKEN(4014, HttpStatus.UNAUTHORIZED, "Token xác minh không hợp lệ hoặc đã hết hạn"),
    INVALID_GOOGLE_TOKEN(4015, HttpStatus.UNAUTHORIZED, "Google token không hợp lệ"),
    SESSION_EXPIRED(4016, HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn"),
    TOKEN_REUSE_DETECTED(4017, HttpStatus.UNAUTHORIZED, "Phát hiện sử dụng lại token — tất cả phiên đã bị thu hồi"),
    INVALID_OTP(4018, HttpStatus.BAD_REQUEST, "Mã OTP không đúng"),
    OTP_RATE_LIMITED(4019, HttpStatus.TOO_MANY_REQUESTS, "Vui lòng chờ trước khi gửi lại OTP"),
    PROFILE_INCOMPLETE(4020, HttpStatus.FORBIDDEN, "Vui lòng hoàn tất thông tin cá nhân"),
    EMAIL_ALREADY_EXISTS(4090, HttpStatus.CONFLICT, "Email đã được sử dụng"),
    USERNAME_ALREADY_EXISTS(4091, HttpStatus.CONFLICT, "Username đã được sử dụng"),
    ACCOUNT_NOT_ACTIVE(4021, HttpStatus.FORBIDDEN, "Tài khoản không ở trạng thái hoạt động"),
    CURRENT_PASSWORD_REQUIRED(4022, HttpStatus.BAD_REQUEST, "Vui lòng nhập mật khẩu hiện tại"),
    CURRENT_PASSWORD_INCORRECT(4023, HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng"),
    PASSWORD_SAME_AS_OLD(4024, HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại"),
    PASSWORD_CONFIRMATION_MISMATCH(4025, HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp"),

    ROLE_NOT_FOUND(5001, HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy role"),
    EMAIL_SERVICE_UNAVAILABLE(5030, HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ email không khả dụng");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
