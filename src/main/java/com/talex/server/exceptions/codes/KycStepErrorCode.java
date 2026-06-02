package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum KycStepErrorCode {
    KYC_STEP_NOT_FOUND(4060, HttpStatus.NOT_FOUND, "Không tìm thấy bước KYC"),
    INVALID_STEP_TYPE(4061, HttpStatus.BAD_REQUEST, "Loại bước KYC không hợp lệ"),
    KYC_STEP_NOT_SUPPORTED(4062, HttpStatus.BAD_REQUEST, "Loại bước KYC chưa được hỗ trợ"),
    KYC_STEP_PROCESSING_FAILED(4063, HttpStatus.BAD_REQUEST, "Xử lý bước KYC thất bại"),
    KYC_STEP_PROVIDER_ERROR(4064, HttpStatus.BAD_GATEWAY, "Lỗi nhà cung cấp dịch vụ KYC");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
