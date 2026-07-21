package com.talex.server.exceptions.details.payment;

import com.talex.server.exceptions.codes.payment.PaymentErrorCode;
import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {
    private final PaymentErrorCode errorCode;

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public PaymentException(PaymentErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
