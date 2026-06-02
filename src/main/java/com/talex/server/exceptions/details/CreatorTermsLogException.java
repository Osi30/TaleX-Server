package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.CreatorTermsLogErrorCode;
import lombok.Getter;

@Getter
public class CreatorTermsLogException extends RuntimeException {
    private final CreatorTermsLogErrorCode errorCode;

    public CreatorTermsLogException(CreatorTermsLogErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CreatorTermsLogException(CreatorTermsLogErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CreatorTermsLogException(CreatorTermsLogErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
