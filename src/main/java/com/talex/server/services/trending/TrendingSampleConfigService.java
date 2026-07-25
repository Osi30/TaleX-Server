package com.talex.server.services.trending;

import com.talex.server.dtos.recommend.TrendingSampleConfigReq;
import com.talex.server.dtos.recommend.TrendingSampleConfigRes;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface TrendingSampleConfigService {

    TrendingSampleConfigRes getConfig();

    TrendingSampleConfigRes createConfig(TrendingSampleConfigReq req) throws BadRequestException;

    TrendingSampleConfigRes updateConfig(TrendingSampleConfigReq req) throws BadRequestException;

    void incrementBatchAndRecalculateThresholdIfNeeded(int completedCount, List<Double> historicalWilsonScores);

    TrendingSampleConfigRes forceRecalculateThreshold();
}