package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.KycSessionErrorCode;
import lombok.Getter;

@Getter
public class KycSessionException extends RuntimeException {
    private final KycSessionErrorCode errorCode;

    public KycSessionException(KycSessionErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public KycSessionException(KycSessionErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
