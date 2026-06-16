package com.talex.server.services.impls;

import com.talex.server.dtos.requests.CreatorIdentityRequestDto;
import com.talex.server.dtos.responses.CreatorIdentityResponseDto;
import com.talex.server.entities.Creator;
import com.talex.server.entities.CreatorIdentity;
import com.talex.server.exceptions.codes.CreatorIdentityErrorCode;
import com.talex.server.exceptions.details.CreatorIdentityException;
import com.talex.server.mappers.ICreatorIdentityMapper;
import com.talex.server.repositories.creator.CreatorIdentityRepository;
import com.talex.server.services.ICreatorIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatorIdentityService implements ICreatorIdentityService {
    private final CreatorIdentityRepository repository;
    private final ICreatorIdentityMapper mapper;

    @Override
    public void create(Creator creator) {
        // Check if Creator already has an identity
        Optional<CreatorIdentity> creatorIdentity = repository
                .findByCreator_CreatorId(creator.getCreatorId());
        if (creatorIdentity.isPresent()) {
            return;
        }

        CreatorIdentity entity = CreatorIdentity.builder()
                .creator(creator)
                .build();

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorIdentityResponseDto getById(String id) {
        return mapper.toResponseDto(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public CreatorIdentityResponseDto getByAccountId(String accountId) {
        CreatorIdentity entity = repository.findByCreator_Account_AccountId(UUID.fromString(accountId))
                .orElseThrow(
                        () -> new CreatorIdentityException(
                                CreatorIdentityErrorCode.CREATOR_IDENTITY_NOT_FOUND,
                                "CreatorIdentity not found for account: " + accountId)
                );
        return mapper.toResponseDto(entity);
    }

    @Override
    public CreatorIdentityResponseDto update(String id, CreatorIdentityRequestDto dto) {
        CreatorIdentity existing = findById(id);

        Optional.ofNullable(dto.getIdNumber()).ifPresent(existing::setIdNumber);
        Optional.ofNullable(dto.getFullName()).ifPresent(existing::setFullName);
        Optional.ofNullable(dto.getDob()).ifPresent(existing::setDob);
        Optional.ofNullable(dto.getSex()).ifPresent(existing::setSex);
        Optional.ofNullable(dto.getAddress()).ifPresent(existing::setAddress);
        Optional.ofNullable(dto.getDoe()).ifPresent(existing::setDoe);
        Optional.ofNullable(dto.getTaxId()).ifPresent(existing::setTaxId);

        CreatorIdentity saved = repository.save(existing);
        return mapper.toResponseDto(saved);
    }

    @Override
    public void delete(String id) {
        CreatorIdentity existing = findById(id);
        repository.delete(existing);
    }

    private CreatorIdentity findById(String id) {
        return repository.findById(id)
                .orElseThrow(
                        () -> new CreatorIdentityException(
                                CreatorIdentityErrorCode.CREATOR_IDENTITY_NOT_FOUND,
                                "CreatorIdentity not found for creator: " + id)
                );
    }
}
