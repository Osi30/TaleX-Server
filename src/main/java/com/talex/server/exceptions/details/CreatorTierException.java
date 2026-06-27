package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.CreatorTierErrorCode;
import lombok.Getter;

@Getter
public class CreatorTierException extends RuntimeException {
    private final CreatorTierErrorCode errorCode;

    public CreatorTierException(CreatorTierErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CreatorTierException(CreatorTierErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
