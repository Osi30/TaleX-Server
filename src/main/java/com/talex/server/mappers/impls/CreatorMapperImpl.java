package com.talex.server.mappers.impls;

import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.mappers.ICreatorMapper;
import org.springframework.stereotype.Component;

@Component
public class CreatorMapperImpl implements ICreatorMapper {

    @Override
    public CreatorResponseDto toResponseDto(Creator creator) {
        if (creator == null)
            return null;

        return CreatorResponseDto.builder()
                .creatorId(creator.getCreatorId())
                .likes(creator.getLikes())
                .bookmarks(creator.getBookmarks())
                .comments(creator.getComments())
                .shares(creator.getShares())
                .totalViews(creator.getTotalViews())
                .followerCount(creator.getAccount().getTotalFollowers())
                .totalWatchTime(creator.getTotalWatchTime())
                .createdAt(creator.getCreatedAt())
                .updatedAt(creator.getUpdatedAt())
                .build();
    }
}
