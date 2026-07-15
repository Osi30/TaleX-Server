package com.talex.server.mappers.creator.impls;

import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.mappers.creator.ICreatorMapper;
import org.springframework.stereotype.Component;

@Component
public class CreatorMapperImpl implements ICreatorMapper {

    @Override
    public CreatorResponseDto toResponseDto(Creator creator) {
        if (creator == null)
            return null;

        return CreatorResponseDto.builder()
                .creatorId(creator.getCreatorId())
                .likes(creator.getAnalyticData().getLikes())
                .bookmarks(creator.getAnalyticData().getBookmarks())
                .comments(creator.getAnalyticData().getComments())
                .shares(creator.getAnalyticData().getShares())
                .totalViews(creator.getAnalyticData().getViews())
                .followToCount(creator.getAccount().getTotalFollowersTo())
                .followerCount(creator.getAccount().getTotalFollowersBy())
                .totalWatchTime(creator.getAnalyticData().getWatchTime())
                .createdAt(creator.getCreatedAt())
                .updatedAt(creator.getUpdatedAt())
                .build();
    }
}
