package com.talex.server.mappers.impls;

import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.entities.CreatorTermsLog;
import com.talex.server.mappers.ICreatorTermsLogMapper;
import org.springframework.stereotype.Component;

@Component
public class CreatorTermsLogMapperImpl implements ICreatorTermsLogMapper {

    @Override
    public CreatorTermsLogResponseDto toResponseDto(CreatorTermsLog entity) {
        if (entity == null)
            return null;

        return CreatorTermsLogResponseDto.builder()
                .id(entity.getId())
                .creatorId(entity.getCreator() != null ? entity.getCreator().getCreatorId() : null)
                .versionId(entity.getVersion() != null ? entity.getVersion().getId() : null)
                .acceptedAt(entity.getAcceptedAt())
                .build();
    }

    @Override
    public CreatorTermsLog toEntity(CreatorTermsLogRequestDto dto) {
        if (dto == null)
            return null;

        CreatorTermsLog entity = CreatorTermsLog.builder().build();
        return entity;
    }
}
