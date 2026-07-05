package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.PaymentErrorCode;
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
