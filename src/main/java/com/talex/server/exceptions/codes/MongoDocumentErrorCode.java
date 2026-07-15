package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MongoDocumentErrorCode {
    ASYNC_PROCESSING_ERROR(5001, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi xử lý hàng đợi tương tác hệ thống"),
    WORKER_PROCESSING_ERROR(5002, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi nghiệp vụ chuyên biệt của Worker"),
    OTHER_PROCESSING_ERROR(5004, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không lường trước"),
   ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
