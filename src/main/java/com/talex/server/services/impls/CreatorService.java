package com.talex.server.services.impls;

import com.talex.server.dtos.requests.CreatorRegisterDto;
import com.talex.server.dtos.requests.CreatorRequestDto;
import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.entities.Creator;
import com.talex.server.exceptions.details.CreatorException;
import com.talex.server.exceptions.codes.CreatorErrorCode;
import com.talex.server.mappers.ICreatorMapper;
import com.talex.server.repositories.CreatorRepository;
import com.talex.server.services.ICreatorIdentityService;
import com.talex.server.services.ICreatorService;
import com.talex.server.services.ICreatorTermsLogService;
import com.talex.server.services.IKycSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatorService implements ICreatorService {
    private final ICreatorTermsLogService creatorTermsLogService;
    private final ICreatorIdentityService creatorIdentityService;
    private final IKycSessionService kycSessionService;
    private final CreatorRepository creatorRepository;
    private final ICreatorMapper creatorMapper;

    @Override
    @Transactional
    public String createCreator(CreatorRegisterDto dto) {
//        Creator creator = findCreatorByAccountId(dto.getAccountId());
        Creator creator;

        // Đã đồng ý điều khoản
        if (Boolean.TRUE.equals(dto.getIsAcceptTermAlready())) {
            creator = findById(dto.getAccountId());
        }
        // Chưa đồng ý điều khoản
        else {
            // 1. Creator
            Creator entity = new Creator();
            creator = creatorRepository.save(entity);

            // 2. Log
            creatorTermsLogService.create(CreatorTermsLogRequestDto.builder()
                    .versionId(dto.getTermsId())
                    .creator(creator)
                    .build());

            // 3. Identity
            creatorIdentityService.create(creator);
        }

        // 4. Session
        return kycSessionService.createSession(creator);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorResponseDto getById(String id) {
        Creator creator = findById(id);
        return creatorMapper.toResponseDto(creator);
    }

    @Override
    public CreatorResponseDto updateCreator(String id, CreatorRequestDto dto) {
        Creator existing = findById(id);

        existing.setNickname(dto.getNickname());
        existing.setBio(dto.getBio());

        Creator saved = creatorRepository.save(existing);
        return creatorMapper.toResponseDto(saved);
    }

    @Override
    public void deleteCreator(String id) {
        Creator existing = findById(id);
        creatorRepository.delete(existing);
    }

    private Creator findById(String id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new CreatorException(CreatorErrorCode.CREATOR_NOT_FOUND,
                        "Creator không tồn tại với id: " + id));
    }

//    private Creator findCreatorByAccountId(String id) {
//        return creatorRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Creator not found with id: " + id));
//    }
}
