package com.talex.server.mappers.subscription.impls;

import com.talex.server.dtos.revenue.request.RuleXCalculationRequestDto;
import com.talex.server.dtos.subscription.dtos.SubscriptionGroupKey;
import com.talex.server.dtos.subscription.dtos.SubscriptionStatRawData;
import com.talex.server.dtos.subscription.dtos.UserStreamGroup;
import com.talex.server.mappers.subscription.RuleXRequestMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RuleXRequestMapperImpl implements RuleXRequestMapper {

    @Override
    public List<RuleXCalculationRequestDto> aggregate(List<SubscriptionStatRawData> rawDataList, double alpha) {
        Map<SubscriptionGroupKey, UserStreamGroup> groupedMap = new LinkedHashMap<>();

        // 1. Phân loại và tích lũy dữ liệu vào từng nhóm
        for (SubscriptionStatRawData raw : rawDataList) {
            SubscriptionGroupKey groupKey = raw.toGroupKey();
            groupedMap.computeIfAbsent(groupKey, k -> new UserStreamGroup())
                    .addStat(raw.getUserId(), raw.creatorId(), raw.episodeId(), raw.views());
        }

        // 2. Chuyển đổi các nhóm tích lũy thành DTO kết quả
        List<RuleXCalculationRequestDto> requestDtos = new ArrayList<>();
        for (Map.Entry<SubscriptionGroupKey, UserStreamGroup> entry : groupedMap.entrySet()) {
            SubscriptionGroupKey groupKey = entry.getKey();
            UserStreamGroup accumulator = entry.getValue();

            requestDtos.add(RuleXCalculationRequestDto.builder()
                    .alpha(alpha)
                    .subscriptionFee(groupKey.subscriptionFee())
                    .users(accumulator.toUserStreamRequestDtos())
                    .build());
        }

        return requestDtos;
    }
}
