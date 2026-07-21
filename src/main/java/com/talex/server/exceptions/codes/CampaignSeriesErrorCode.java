package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CampaignSeriesErrorCode {
    NOT_FOUND(4041, HttpStatus.NOT_FOUND, "Không tìm thấy chuỗi chiến dịch"),
    INVALID_STATUS_TRANSITION(4002, HttpStatus.BAD_REQUEST, "Chuyển đổi trạng thái chuỗi chiến dịch không hợp lệ"),
    CANNOT_CANCEL(4003, HttpStatus.BAD_REQUEST, "Không thể hủy chuỗi chiến dịch ở trạng thái hiện tại");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}