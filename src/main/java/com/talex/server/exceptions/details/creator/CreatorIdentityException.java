package com.talex.server.exceptions.details.creator;

import com.talex.server.exceptions.codes.creator.CreatorIdentityErrorCode;
import lombok.Getter;

@Getter
public class CreatorIdentityException extends RuntimeException {
    private final CreatorIdentityErrorCode errorCode;

    public CreatorIdentityException(CreatorIdentityErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CreatorIdentityException(CreatorIdentityErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CreatorIdentityErrorCode getErrorCode() {
        return errorCode;
    }
}
