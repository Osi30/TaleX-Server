package com.talex.server.services.trending;

import com.talex.server.dtos.recommend.request.TrendingSampleConfigReq;
import com.talex.server.dtos.recommend.response.TrendingSampleConfigRes;
import org.apache.coyote.BadRequestException;

public interface TrendingSampleConfigService {

    TrendingSampleConfigRes getConfig();

    TrendingSampleConfigRes createConfig(TrendingSampleConfigReq req) throws BadRequestException;

    TrendingSampleConfigRes updateConfig(TrendingSampleConfigReq req) throws BadRequestException;

    void incrementBatchAndRecalculateThresholdIfNeeded(int completedCount);

    TrendingSampleConfigRes forceRecalculateThreshold();
}