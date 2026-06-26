package com.talex.server.mappers.impls;

import com.talex.server.dtos.requests.creator.CreatorRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.entities.Creator;
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
                .nickname(creator.getNickname())
                .bio(creator.getBio())
                .createdAt(creator.getCreatedAt())
                .updatedAt(creator.getUpdatedAt())
                .build();
    }

    @Override
    public Creator toEntity(CreatorRequestDto dto) {
        if (dto == null)
            return null;

        return Creator.builder()
                .nickname(dto.getNickname())
                .bio(dto.getBio())
                .build();
    }
}
