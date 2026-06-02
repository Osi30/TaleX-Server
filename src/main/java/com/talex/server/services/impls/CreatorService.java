package com.talex.server.services.impls;

import com.talex.server.dtos.requests.CreatorRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.entities.Creator;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.mappers.ICreatorMapper;
import com.talex.server.repositories.CreatorRepository;
import com.talex.server.services.ICreatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreatorService implements ICreatorService {
    private final CreatorRepository creatorRepository;
    private final ICreatorMapper creatorMapper;

    @Override
    public CreatorResponseDto createCreator(CreatorRequestDto dto) {
        Creator entity = creatorMapper.toEntity(dto);
        Creator saved = creatorRepository.save(entity);
        return creatorMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorResponseDto getById(String id) {
        Creator creator = creatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found with id: " + id));
        return creatorMapper.toResponseDto(creator);
    }

    @Override
    public CreatorResponseDto updateCreator(String id, CreatorRequestDto dto) {
        Creator existing = creatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found with id: " + id));

        existing.setIsVerified(dto.getIsVerified());
        existing.setVerificationTime(dto.getVerificationTime());
        existing.setNickname(dto.getNickname());
        existing.setBio(dto.getBio());

        Creator saved = creatorRepository.save(existing);
        return creatorMapper.toResponseDto(saved);
    }

    @Override
    public void deleteCreator(String id) {
        Creator existing = creatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Creator not found with id: " + id));
        creatorRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreatorResponseDto> listCreators(Map<String, Object> params) {
        return creatorRepository.findAll().stream()
                .map(creatorMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
