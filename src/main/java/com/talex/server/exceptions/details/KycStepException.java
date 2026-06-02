package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.KycStepErrorCode;
import lombok.Getter;

@Getter
public class KycStepException extends RuntimeException {
    private final KycStepErrorCode errorCode;

    public KycStepException(KycStepErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public KycStepException(KycStepErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public KycStepException(KycStepErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
