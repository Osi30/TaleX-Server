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
    WORKER_DATABASE_UPSERT_FAILED(5002, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi thực thi câu lệnh UPSERT xuống PostgreSQL"),

    SAVING_DATABASE_ERROR(4001, HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),

    FOLLOW_SELF_NOT_ALLOWED(4010, HttpStatus.BAD_REQUEST, "Bạn không thể tự theo dõi chính mình."),
    FOLLOW_ALREADY_EXISTS(4011, HttpStatus.CONFLICT, "Bạn đã theo dõi tài khoản này rồi."),
    FOLLOW_NOT_FOUND(4012, HttpStatus.NOT_FOUND, "Mối quan hệ theo dõi không tồn tại."),
    ACCOUNT_NOT_FOUND(4013, HttpStatus.NOT_FOUND, "Tài khoản không tồn tại trong hệ thống."),

    LIKE_NOT_FOUND(4014, HttpStatus.NOT_FOUND, "Mối quan hệ yêu thích không tồn tại."),
    LIKE_ALREADY_EXISTS(4015, HttpStatus.CONFLICT, "Bạn đã thích tập phim này rồi."),

            ;

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}