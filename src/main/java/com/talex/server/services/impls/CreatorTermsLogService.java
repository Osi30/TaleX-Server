package com.talex.server.services.impls;

import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.entities.Creator;
import com.talex.server.entities.CreatorTermsLog;
import com.talex.server.entities.TermsVersion;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.mappers.ICreatorTermsLogMapper;
import com.talex.server.repositories.CreatorRepository;
import com.talex.server.repositories.CreatorTermsLogRepository;
import com.talex.server.repositories.TermsVersionRepository;
import com.talex.server.services.ICreatorTermsLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreatorTermsLogService implements ICreatorTermsLogService {
    private final CreatorTermsLogRepository logRepository;
    private final CreatorRepository creatorRepository;
    private final TermsVersionRepository termsRepository;
    private final ICreatorTermsLogMapper mapper;

    @Override
    public CreatorTermsLogResponseDto create(CreatorTermsLogRequestDto dto) {
        Creator creator = creatorRepository.findById(dto.getCreatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found with id: " + dto.getCreatorId()));
        TermsVersion version = termsRepository.findById(dto.getVersionId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("TermsVersion not found with id: " + dto.getVersionId()));

        CreatorTermsLog log = CreatorTermsLog.builder()
                .creator(creator)
                .version(version)
                .build();

        CreatorTermsLog saved = logRepository.save(log);
        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorTermsLogResponseDto getById(String id) {
        CreatorTermsLog log = logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CreatorTermsLog not found with id: " + id));
        return mapper.toResponseDto(log);
    }

    @Override
    public void delete(String id) {
        CreatorTermsLog log = logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CreatorTermsLog not found with id: " + id));
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
