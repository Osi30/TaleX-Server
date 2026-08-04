package com.talex.server.exceptions.codes.report;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ModerationErrorCode {
    REPORT_NOT_FOUND(4041, HttpStatus.NOT_FOUND, "Không tìm thấy đơn báo cáo"),
    TICKET_NOT_FOUND(4042, HttpStatus.NOT_FOUND, "Không tìm thấy ticket kiểm duyệt"),
    PENALTY_NOT_FOUND(4043, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin hình phạt"),
    APPEAL_NOT_FOUND(4044, HttpStatus.NOT_FOUND, "Không tìm thấy đơn khiếu nại"),
    TARGET_NOT_FOUND(4045, HttpStatus.NOT_FOUND, "Không tìm thấy mục tiêu khiếu nại"),
    DUPLICATE_REPORT(4001, HttpStatus.BAD_REQUEST, "Bạn đã gửi báo cáo cho nội dung này trước đó và đang chờ xử lý"),
    ALREADY_APPEALED(4002, HttpStatus.BAD_REQUEST, "Hình phạt này đã được tạo đơn khiếu nại trước đó"),
    APPEAL_EXPIRED(4003, HttpStatus.BAD_REQUEST, "Đã quá thời hạn 7 ngày để gửi đơn khiếu nại cho hình phạt này"),
    INVALID_STATUS(4004, HttpStatus.BAD_REQUEST, "Trạng thái xử lý không hợp lệ hoặc đã kết thúc"),
    INVALID_TARGET(4005, HttpStatus.BAD_REQUEST, "Mục tiêu báo cáo không hợp lệ"),
    INVALID_TARGET_TYPE(4006, HttpStatus.BAD_REQUEST, "Loại mục tiêu báo cáo không hợp lệ"),
    UNAUTHORIZED_ACTION(4031, HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác kiểm duyệt này");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}