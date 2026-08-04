package com.talex.server.exceptions.details.campaign;

import com.talex.server.exceptions.codes.campaign.CampaignSeriesErrorCode;
import lombok.Getter;

@Getter
public class CampaignSeriesException extends RuntimeException {
    private final CampaignSeriesErrorCode errorCode;

    public CampaignSeriesException(CampaignSeriesErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CampaignSeriesException(CampaignSeriesErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}