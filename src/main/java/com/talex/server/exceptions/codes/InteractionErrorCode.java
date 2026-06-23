package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum InteractionErrorCode {
    ASYNC_PROCESSING_ERROR(5001, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi xử lý hàng đợi tương tác hệ thống"),
    WORKER_PROCESSING_ERROR(5002, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi nghiệp vụ chuyên biệt của Worker"),
    KAFKA_PROCESSING_ERROR(5003, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi nghiệp vụ chuyên biệt của Kafka"),
    OTHER_PROCESSING_ERROR(5004, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không lường trước"),

    WORKER_EPISODE_NOT_FOUND(4043, HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu Episode gốc tại Postgres"),
    WORKER_ACTIVE_SUB_NOT_FOUND(4044, HttpStatus.NOT_FOUND, "Người dùng không có gói subscription hợp lệ tại thời điểm tương tác"),
    WORKER_DATABASE_UPSERT_FAILED(5002, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi thực thi câu lệnh UPSERT xuống PostgreSQL");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}