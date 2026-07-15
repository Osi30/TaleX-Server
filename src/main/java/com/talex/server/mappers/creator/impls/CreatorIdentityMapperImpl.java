package com.talex.server.mappers.creator.impls;

import com.talex.server.dtos.requests.creator.CreatorIdentityRequestDto;
import com.talex.server.dtos.responses.creator.CreatorIdentityResponseDto;
import com.talex.server.entities.creator.CreatorIdentity;
import com.talex.server.mappers.creator.ICreatorIdentityMapper;
import org.springframework.stereotype.Component;

@Component
public class CreatorIdentityMapperImpl implements ICreatorIdentityMapper {

    @Override
    public CreatorIdentityResponseDto toResponseDto(CreatorIdentity entity) {
        if (entity == null)
            return null;

        CreatorIdentityResponseDto.CreatorIdentityResponseDtoBuilder builder = CreatorIdentityResponseDto.builder()
                .creatorIdentityId(entity.getCreatorIdentityId())
                .idNumber(entity.getIdNumber())
                .fullName(entity.getFullName())
                .dob(entity.getDob())
                .sex(entity.getSex())
                .address(entity.getAddress())
                .doe(entity.getDoe())
                .taxId(entity.getTaxId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());

        if (entity.getCreator() != null) {
            builder.creatorId(entity.getCreator().getCreatorId());
        }

        return builder.build();
    }

    @Override
    public CreatorIdentity toEntity(CreatorIdentityRequestDto dto) {
        if (dto == null)
            return null;

        return CreatorIdentity.builder()
                .idNumber(dto.getIdNumber())
                .fullName(dto.getFullName())
                .dob(dto.getDob())
                .sex(dto.getSex())
                .address(dto.getAddress())
                .doe(dto.getDoe())
                .taxId(dto.getTaxId())
                .build();
    }
}
