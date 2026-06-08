package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum KycStepErrorCode {
    KYC_STEP_NOT_FOUND(4041, HttpStatus.NOT_FOUND, "Không tìm thấy bước KYC"),
    INVALID_STEP_TYPE(4001, HttpStatus.BAD_REQUEST, "Loại bước KYC không hợp lệ"),
    KYC_STEP_NOT_SUPPORTED(4002, HttpStatus.BAD_REQUEST, "Loại bước KYC chưa được hỗ trợ"),
    KYC_STEP_PROCESSING_FAILED(4003, HttpStatus.BAD_REQUEST, "Xử lý bước KYC thất bại"),
    KYC_STEP_ID_NUMBER_ALREADY_EXIST(4004, HttpStatus.BAD_REQUEST, "CCCD Đã được đăng kí ở một tài khoản khác"),
    KYC_STEP_PROVIDER_ERROR(5021, HttpStatus.BAD_GATEWAY, "Lỗi nhà cung cấp dịch vụ KYC");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
