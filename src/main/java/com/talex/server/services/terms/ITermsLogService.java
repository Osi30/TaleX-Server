package com.talex.server.services.terms;

import com.talex.server.dtos.requests.terms.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.entities.Account;

import java.util.List;
import java.util.UUID;

public interface ITermsLogService {
    void create(Account account, CreatorTermsLogRequestDto dto);

    void create(UUID accountId, CreatorTermsLogRequestDto dto);

    CreatorTermsLogResponseDto getById(String id);

    List<CreatorTermsLogResponseDto> listByAccount(String creatorId);

    boolean existsByAccountAndTerm(UUID accountId, String termId);
}
