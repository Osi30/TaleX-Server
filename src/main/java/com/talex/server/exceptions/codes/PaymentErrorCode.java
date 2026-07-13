package com.talex.server.exceptions.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode {
    SUBSCRIPTION_NOT_FOUND_FOR_ORDER(4041, HttpStatus.NOT_FOUND, "Gói dịch vụ không tồn tại"),
    ORDER_NOT_FOUND(4042, HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"),
    ORDER_EXPIRED(4001, HttpStatus.BAD_REQUEST, "Đơn hàng đã hết thời gian thanh toán"),
    ORDER_NOT_OWNED(4031, HttpStatus.FORBIDDEN, "Đơn hàng không thuộc về tài khoản này"),
    ORDER_ALREADY_COMPLETED(4002, HttpStatus.BAD_REQUEST, "Đơn hàng đã được thanh toán"),
    CONTENT_ALREADY_OWNED(4003, HttpStatus.CONFLICT, "Bạn đã sở hữu nội dung này"),
    INVALID_ITEM_TYPE(4004, HttpStatus.BAD_REQUEST, "Loại nội dung không hợp lệ, chỉ chấp nhận EPISODE hoặc COMBO"),
    ORDER_NOT_CANCELLABLE(4005, HttpStatus.BAD_REQUEST, "Chỉ có thể hủy đơn hàng đang chờ thanh toán"),
    ORDER_NOT_FULLY_COVERED_BY_COIN(4006, HttpStatus.BAD_REQUEST,
            "Số Coin hiện tại chưa đủ trả hết đơn hàng, vui lòng thanh toán phần còn lại qua SePay");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
