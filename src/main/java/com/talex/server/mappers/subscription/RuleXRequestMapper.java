package com.talex.server.mappers.subscription;

import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.subscription.dtos.SubscriptionStatRawData;

import java.util.List;

public interface RuleXRequestMapper {
    /**
     * Gom nhóm danh sách raw data theo (Price, Duration) và map thành danh sách RuleXCalculationRequestDto
     */
    List<RuleXCalculationRequestDto> aggregate(List<SubscriptionStatRawData> rawDataList, double alpha);


}
