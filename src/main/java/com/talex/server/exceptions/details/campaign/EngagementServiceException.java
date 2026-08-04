package com.talex.server.exceptions.details.campaign;

import com.talex.server.exceptions.codes.campaign.EngagementErrorCode;
import lombok.Getter;

@Getter
public class EngagementServiceException extends RuntimeException {
    private final EngagementErrorCode errorCode;

    public EngagementServiceException(EngagementErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public EngagementServiceException(EngagementErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public EngagementErrorCode getErrorCode() {
        return errorCode;
    }
}
