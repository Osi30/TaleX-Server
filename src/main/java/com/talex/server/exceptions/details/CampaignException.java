package com.talex.server.exceptions.details;

import com.talex.server.exceptions.codes.CampaignErrorCode;
import lombok.Getter;

@Getter
public class CampaignException extends RuntimeException {
    private final CampaignErrorCode errorCode;

    public CampaignException(CampaignErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CampaignException(CampaignErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CampaignErrorCode getErrorCode() {
        return errorCode;
    }
}
