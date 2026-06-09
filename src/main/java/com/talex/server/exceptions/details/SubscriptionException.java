package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.SubscriptionErrorCode;
import lombok.Getter;

@Getter
public class SubscriptionException extends RuntimeException {
    private final SubscriptionErrorCode errorCode;

    public SubscriptionException(SubscriptionErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public SubscriptionException(SubscriptionErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
