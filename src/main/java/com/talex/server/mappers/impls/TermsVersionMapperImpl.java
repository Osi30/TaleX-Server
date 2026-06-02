package com.talex.server.mappers.impls;

import com.talex.server.dtos.requests.TermsVersionRequestDto;
import com.talex.server.dtos.responses.TermsVersionResponseDto;
import com.talex.server.entities.TermsVersion;
import com.talex.server.mappers.ITermsVersionMapper;
import org.springframework.stereotype.Component;

@Component
public class TermsVersionMapperImpl implements ITermsVersionMapper {

    @Override
    public TermsVersionResponseDto toResponseDto(TermsVersion entity) {
        if (entity == null)
            return null;

        return TermsVersionResponseDto.builder()
                .id(entity.getId())
                .version(entity.getVersion())
                .type(entity.getType())
                .content(entity.getContent())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public TermsVersion toEntity(TermsVersionRequestDto dto) {
        if (dto == null)
            return null;

        return TermsVersion.builder()
                .version(dto.getVersion())
                .type(dto.getType())
                .content(dto.getContent())
                .isActive(dto.getIsActive())
                .build();
    }
}
