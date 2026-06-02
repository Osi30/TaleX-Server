package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.TermsVersionErrorCode;
import lombok.Getter;

@Getter
public class TermVersionException extends RuntimeException {
    private final TermsVersionErrorCode errorCode;

    public TermVersionException(TermsVersionErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public TermVersionException(TermsVersionErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TermsVersionErrorCode getErrorCode() {
        return errorCode;
    }
}
