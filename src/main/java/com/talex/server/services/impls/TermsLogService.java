package com.talex.server.services.impls;

import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.entities.TermsLog;
import com.talex.server.entities.TermsVersion;
import com.talex.server.exceptions.codes.CreatorTermsLogErrorCode;
import com.talex.server.exceptions.details.CreatorTermsLogException;
import com.talex.server.mappers.ITermsLogMapper;
import com.talex.server.repositories.TermsLogRepository;
import com.talex.server.services.ITermsLogService;
import com.talex.server.services.ITermsVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermsLogService implements ITermsLogService {
    private final ITermsVersionService termsVersionService;
    private final TermsLogRepository logRepository;
    private final ITermsLogMapper mapper;

    @Override
    public void create(CreatorTermsLogRequestDto dto) {
        TermsVersion version = termsVersionService.findById(dto.getVersionId());
        if (!version.getIsActive()) {
            throw new CreatorTermsLogException(
                    CreatorTermsLogErrorCode.CREATOR_TERMS_LOG_INACTIVE_VERSION,
                    "Bản điều khoản " + dto.getVersionId() + " chưa được kích hoạt.");
        }

        TermsLog log = TermsLog.builder()
                .account(dto.getCreator().getAccount())
                .version(version)
                .build();

        logRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorTermsLogResponseDto getById(String id) {
        TermsLog log = logRepository.findById(id)
                .orElseThrow(() -> new CreatorTermsLogException(
                        CreatorTermsLogErrorCode.CREATOR_TERMS_LOG_NOT_FOUND,
                        "CreatorTermsLog không tồn tại với id: " + id));
        return mapper.toResponseDto(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreatorTermsLogResponseDto> listByAccount(String accountId) {
        return logRepository.findByAccount_AccountId(UUID.fromString(accountId)).stream()
                .map(mapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
