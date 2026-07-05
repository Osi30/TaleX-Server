package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.PaymentProfileErrorCode;
import lombok.Getter;

@Getter
public class PaymentProfileException extends RuntimeException {
    private final PaymentProfileErrorCode errorCode;

    public PaymentProfileException(PaymentProfileErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public PaymentProfileException(PaymentProfileErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
