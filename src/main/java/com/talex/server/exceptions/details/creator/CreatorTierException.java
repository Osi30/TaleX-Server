package com.talex.server.exceptions.details.creator;

import com.talex.server.exceptions.codes.creator.CreatorTierErrorCode;
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
