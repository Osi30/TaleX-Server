package com.talex.server.mappers.impls;

import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.entities.term.TermsLog;
import com.talex.server.mappers.ITermsLogMapper;
import org.springframework.stereotype.Component;

@Component
public class TermsLogMapperImpl implements ITermsLogMapper {

    @Override
    public CreatorTermsLogResponseDto toResponseDto(TermsLog entity) {
        if (entity == null)
            return null;

        return CreatorTermsLogResponseDto.builder()
                .id(entity.getId())
                .accountId(entity.getAccount() != null ? entity.getAccount().getAccountId().toString() : null)
                .versionId(entity.getVersion() != null ? entity.getVersion().getId() : null)
                .acceptedAt(entity.getAcceptedAt())
                .build();
    }

    @Override
    public TermsLog toEntity(CreatorTermsLogRequestDto dto) {
        if (dto == null)
            return null;

        return TermsLog.builder().build();
    }
}
