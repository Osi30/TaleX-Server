package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.CreatorErrorCode;
import lombok.Getter;

@Getter
public class CreatorException extends RuntimeException {
    private final CreatorErrorCode errorCode;

    public CreatorException(CreatorErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CreatorException(CreatorErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CreatorException(CreatorErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
