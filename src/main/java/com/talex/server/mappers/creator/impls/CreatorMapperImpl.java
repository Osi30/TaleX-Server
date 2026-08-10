package com.talex.server.mappers.creator.impls;

import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.mappers.creator.CreatorMapper;
import com.talex.server.services.creator.CreatorTierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreatorMapperImpl implements CreatorMapper {
    private final CreatorTierService creatorTierService;

    @Override
    public CreatorResponseDto toResponseDto(Creator creator) {
        if (creator == null)
            return null;

        return CreatorResponseDto.builder()
                .creatorId(creator.getCreatorId())
                .analyticData(creator.getAnalyticData())
                .followToCount(creator.getAccount().getTotalFollowersTo())
                .followerCount(creator.getAccount().getTotalFollowersBy())
                .creatorTier(creatorTierService.getCurrentEligibleTier(
                        creator.getAccount().getTotalFollowersBy(),
                        creator.getAnalyticData().getViews(),
                        creator.getAnalyticData().getWatchTime()
                ))
                .createdAt(creator.getCreatedAt())
                .updatedAt(creator.getUpdatedAt())
                .build();
    }
}
