package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorIdentityRequestDto;
import com.talex.server.dtos.requests.creator.CreatorVerifiedResultDto;
import com.talex.server.dtos.requests.filters.CreatorIdentityFilterRequestDto;
import com.talex.server.dtos.responses.creator.CreatorIdentityResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorIdentity;
import com.talex.server.enums.creator.CreatorIdentityStatus;
import com.talex.server.exceptions.details.creator.CreatorIdentityException;
import com.talex.server.mappers.creator.CreatorIdentityMapper;
import com.talex.server.repositories.creator.CreatorIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatorIdentityServiceImpl Tests")
class CreatorIdentityServiceImplTest {

    @Mock
    private CreatorIdentityRepository repository;

    @Mock
    private CreatorIdentityMapper mapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private CreatorIdentityServiceImpl service;

    private Creator sampleCreator;
    private CreatorIdentity sampleIdentity;

    @BeforeEach
    void setUp() {
        Account account = Account.builder().accountId(UUID.randomUUID()).build();
        sampleCreator = Creator.builder().creatorId("creator-123").account(account).build();
        sampleIdentity = CreatorIdentity.builder()
                .creatorIdentityId("identity-123")
                .creator(sampleCreator)
                .status(CreatorIdentityStatus.AWAITING_FILL)
                .taxId("OLD_TAX")
                .build();
    }

    @Test
    @DisplayName("create - Creator identity already exists -> return early")
    void create_AlreadyExists() {
        when(repository.findByCreator_CreatorId("creator-123")).thenReturn(Optional.of(sampleIdentity));

        service.create(sampleCreator);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create - Creator identity absent -> saves new identity")
    void create_Success() {
        when(repository.findByCreator_CreatorId("creator-123")).thenReturn(Optional.empty());

        service.create(sampleCreator);

        verify(repository, times(1)).save(argThat(entity ->
                entity.getCreator().equals(sampleCreator) &&
                entity.getStatus() == CreatorIdentityStatus.AWAITING_FILL
        ));
    }

    @Test
    @DisplayName("getById - Found vs Not Found")
    void getById() {
        when(repository.findById("identity-123")).thenReturn(Optional.of(sampleIdentity));
        CreatorIdentityResponseDto dto = CreatorIdentityResponseDto.builder().creatorIdentityId("identity-123").build();
        when(mapper.toResponseDto(sampleIdentity)).thenReturn(dto);

        CreatorIdentityResponseDto res = service.getById("identity-123");
        assertThat(res).isEqualTo(dto);

        when(repository.findById("invalid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("invalid"))
                .isInstanceOf(CreatorIdentityException.class);
    }

    @Test
    @DisplayName("getByAccountId - Found vs Not Found")
    void getByAccountId() {
        UUID accountId = sampleCreator.getAccount().getAccountId();
        when(repository.findByCreator_Account_AccountId(accountId)).thenReturn(Optional.of(sampleIdentity));
        CreatorIdentityResponseDto dto = CreatorIdentityResponseDto.builder().creatorIdentityId("identity-123").build();
        when(mapper.toResponseDto(sampleIdentity)).thenReturn(dto);

        CreatorIdentityResponseDto res = service.getByAccountId(accountId.toString());
        assertThat(res).isEqualTo(dto);

        UUID unknownId = UUID.randomUUID();
        when(repository.findByCreator_Account_AccountId(unknownId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByAccountId(unknownId.toString()))
                .isInstanceOf(CreatorIdentityException.class);
    }

    @Test
    @DisplayName("update - Update fields and status transition when taxId changes")
    void update() {
        when(repository.findById("identity-123")).thenReturn(Optional.of(sampleIdentity));

        CreatorIdentityRequestDto requestDto = CreatorIdentityRequestDto.builder()
                .idNumber("123456789")
                .fullName("John Doe")
                .dob(LocalDate.of(1990, 1, 1))
                .sex("MALE")
                .address("Hanoi")
                .doe(LocalDate.of(2030, 1, 1))
                .taxId("NEW_TAX")
                .build();

        when(repository.save(any(CreatorIdentity.class))).thenAnswer(inv -> inv.getArgument(0));
        CreatorIdentityResponseDto dto = CreatorIdentityResponseDto.builder().creatorIdentityId("identity-123").build();
        when(mapper.toResponseDto(any())).thenReturn(dto);

        CreatorIdentityResponseDto res = service.update("identity-123", requestDto);

        assertThat(res).isEqualTo(dto);
        assertThat(sampleIdentity.getIdNumber()).isEqualTo("123456789");
        assertThat(sampleIdentity.getTaxId()).isEqualTo("NEW_TAX");
        assertThat(sampleIdentity.getStatus()).isEqualTo(CreatorIdentityStatus.PENDING);
    }

    @Test
    @DisplayName("updateVerifiedStatus - APPROVED triggers Kafka event")
    void updateVerifiedStatus_Approved() {
        when(repository.findById("identity-123")).thenReturn(Optional.of(sampleIdentity));

        CreatorVerifiedResultDto verifiedDto = CreatorVerifiedResultDto.builder()
                .status(CreatorIdentityStatus.APPROVED)
                .verifiedNote("All good")
                .build();

        service.updateVerifiedStatus("identity-123", verifiedDto);

        assertThat(sampleIdentity.getStatus()).isEqualTo(CreatorIdentityStatus.APPROVED);
        assertThat(sampleIdentity.getVerifiedAt()).isNotNull();
        assertThat(sampleIdentity.getVerifiedNote()).isEqualTo("All good");

        verify(kafkaTemplate).send("request-to-update-account", sampleCreator.getAccount().getAccountId().toString());
        verify(repository).save(sampleIdentity);
    }

    @Test
    @DisplayName("updateVerifiedStatus - REJECTED status without Kafka event")
    void updateVerifiedStatus_Rejected() {
        when(repository.findById("identity-123")).thenReturn(Optional.of(sampleIdentity));

        CreatorVerifiedResultDto verifiedDto = CreatorVerifiedResultDto.builder()
                .status(CreatorIdentityStatus.REJECTED)
                .verifiedNote("Invalid ID card")
                .build();

        service.updateVerifiedStatus("identity-123", verifiedDto);

        assertThat(sampleIdentity.getStatus()).isEqualTo(CreatorIdentityStatus.REJECTED);
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("updateTaxId - Null or empty taxId throws Exception vs valid taxId success")
    void updateTaxId() {
        UUID accountId = sampleCreator.getAccount().getAccountId();
        assertThatThrownBy(() -> service.updateTaxId(accountId, null))
                .isInstanceOf(CreatorIdentityException.class);
        assertThatThrownBy(() -> service.updateTaxId(accountId, "  "))
                .isInstanceOf(CreatorIdentityException.class);

        when(repository.findByCreator_Account_AccountId(accountId)).thenReturn(Optional.of(sampleIdentity));

        String status = service.updateTaxId(accountId, "TAX123");

        assertThat(status).isEqualTo(CreatorIdentityStatus.PENDING.toString());
        assertThat(sampleIdentity.getTaxId()).isEqualTo("TAX123");
        verify(repository).save(sampleIdentity);
    }

    @Test
    @DisplayName("delete - Removes entity")
    void delete() {
        when(repository.findById("identity-123")).thenReturn(Optional.of(sampleIdentity));

        service.delete("identity-123");

        verify(repository).delete(sampleIdentity);
    }

    @Test
    @DisplayName("filter - Filtering with statuses and sort property normalization")
    void filter() {
        CreatorIdentityFilterRequestDto filterReq = CreatorIdentityFilterRequestDto.builder()
                .page(1)
                .pageSize(10)
                .sortDirection("ASC")
                .sortBy("verifiedAt")
                .statuses(new String[]{"APPROVED", "PENDING"})
                .criteria(java.util.Map.of("idNumber", "123"))
                .build();

        Page<CreatorIdentity> page = new PageImpl<>(List.of(sampleIdentity));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toResponseDto(sampleIdentity)).thenReturn(CreatorIdentityResponseDto.builder().creatorIdentityId("identity-123").build());

        BasePageResponse<CreatorIdentityResponseDto> response = service.filter(filterReq);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPageNumber()).isEqualTo(1);

        // Invalid status string in array throws IllegalArgumentException
        filterReq.setStatuses(new String[]{"INVALID_STATUS"});
        assertThatThrownBy(() -> service.filter(filterReq))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
