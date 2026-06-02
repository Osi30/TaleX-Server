package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FptAIIDRecognitionErrorCode {
    FPT_AI_OCR_STRUCTURE_ERROR(4070, HttpStatus.BAD_REQUEST, "Lỗi cấu trúc dữ liệu từ FPT AI"),
    FPT_AI_FRONT_RECOGNITION_ERROR(4071, HttpStatus.BAD_GATEWAY, "Lỗi nhận diện mặt trước CCCD"),
    FPT_AI_BACK_RECOGNITION_ERROR(4072, HttpStatus.BAD_GATEWAY, "Lỗi nhận diện mặt sau CCCD"),
    FPT_AI_LIVENESS_ERROR(4073, HttpStatus.BAD_GATEWAY, "Lỗi kiểm tra Liveness & FaceMatch"),
    FPT_AI_REQUEST_ERROR(4074, HttpStatus.BAD_GATEWAY, "Lỗi gọi dịch vụ FPT AI");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
