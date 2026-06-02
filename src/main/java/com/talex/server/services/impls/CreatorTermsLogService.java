package com.talex.server.services.impls;

import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.entities.CreatorTermsLog;
import com.talex.server.entities.TermsVersion;
import com.talex.server.exceptions.codes.CreatorTermsLogErrorCode;
import com.talex.server.exceptions.details.CreatorTermsLogException;
import com.talex.server.mappers.ICreatorTermsLogMapper;
import com.talex.server.repositories.CreatorTermsLogRepository;
import com.talex.server.services.ICreatorTermsLogService;
import com.talex.server.services.ITermsVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreatorTermsLogService implements ICreatorTermsLogService {
    private final ITermsVersionService termsVersionService;
    private final CreatorTermsLogRepository logRepository;
    private final ICreatorTermsLogMapper mapper;

    @Override
    public void create(CreatorTermsLogRequestDto dto) {
        TermsVersion version = termsVersionService.findById(dto.getVersionId());
        if (!version.getIsActive()) {
            throw new CreatorTermsLogException(
                    CreatorTermsLogErrorCode.CREATOR_TERMS_LOG_INACTIVE_VERSION,
                    "Bản điều khoản " + dto.getVersionId() + " chưa được kích hoạt.");
        }

        CreatorTermsLog log = CreatorTermsLog.builder()
                .creator(dto.getCreator())
                .version(version)
                .build();

        logRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorTermsLogResponseDto getById(String id) {
        CreatorTermsLog log = logRepository.findById(id)
                .orElseThrow(() -> new CreatorTermsLogException(
                        CreatorTermsLogErrorCode.CREATOR_TERMS_LOG_NOT_FOUND,
                        "CreatorTermsLog không tồn tại với id: " + id));
        return mapper.toResponseDto(log);
    }

    @Override
    public void delete(String id) {
        CreatorTermsLog log = logRepository.findById(id)
                .orElseThrow(() -> new CreatorTermsLogException(
                        CreatorTermsLogErrorCode.CREATOR_TERMS_LOG_NOT_FOUND,
                        "CreatorTermsLog không tồn tại với id: " + id));
        logRepository.delete(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreatorTermsLogResponseDto> listByCreator(String creatorId) {
        return logRepository.findByCreator_CreatorId(creatorId).stream()
                .map(mapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
