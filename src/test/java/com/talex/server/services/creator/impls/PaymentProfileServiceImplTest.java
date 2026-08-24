package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.PaymentProfileRequestDto;
import com.talex.server.dtos.requests.creator.PaymentProfileVerifiedDto;
import com.talex.server.dtos.requests.filters.PaymentProfileFilterRequestDto;
import com.talex.server.dtos.responses.creator.PaymentProfileResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.PaymentProfile;
import com.talex.server.enums.BankBin;
import com.talex.server.enums.creator.PaymentProfileStatus;
import com.talex.server.exceptions.details.payment.PaymentProfileException;
import com.talex.server.mappers.creator.PaymentProfileMapper;
import com.talex.server.repositories.creator.PaymentProfileRepository;
import com.talex.server.services.creator.CreatorService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProfileServiceImpl Tests")
class PaymentProfileServiceImplTest {

    @Mock
    private PaymentProfileRepository repository;
    @Mock
    private CreatorService creatorService;
    @Mock
    private PaymentProfileMapper mapper;

    @InjectMocks
    private PaymentProfileServiceImpl service;

    private UUID accountId;
    private Creator sampleCreator;
    private PaymentProfile sampleProfile;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        Account account = Account.builder().accountId(accountId).build();
        sampleCreator = Creator.builder().creatorId("creator-1").account(account).paymentProfiles(new ArrayList<>()).build();

        sampleProfile = PaymentProfile.builder()
                .paymentProfileId("profile-1")
                .creator(sampleCreator)
                .isPrimary(false)
                .status(PaymentProfileStatus.PENDING)
                .isDeleted(false)
                .build();

        sampleCreator.getPaymentProfiles().add(sampleProfile);
    }

    @Test
    @DisplayName("create - Create primary profile when no primary exists")
    void create_PrimaryProfile() {
        PaymentProfileRequestDto dto = PaymentProfileRequestDto.builder().isPrimary(true).build();
        PaymentProfile entity = PaymentProfile.builder().paymentProfileId("new-profile").creator(sampleCreator).build();

        when(creatorService.getEntityByAccountId(accountId)).thenReturn(sampleCreator);
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        PaymentProfileResponseDto resDto = PaymentProfileResponseDto.builder().paymentProfileId("new-profile").build();
        when(mapper.toResponseDto(entity)).thenReturn(resDto);

        PaymentProfileResponseDto result = service.create(accountId, dto);

        assertThat(result).isEqualTo(resDto);
        assertThat(entity.getIsPrimary()).isTrue();
        assertThat(entity.getStatus()).isEqualTo(PaymentProfileStatus.PENDING);
        verify(repository).unsetOtherPrimary("creator-1", "");
    }

    @Test
    @DisplayName("getById & findById - Found vs Not Found")
    void getByIdAndFindById() {
        when(repository.findByPaymentProfileIdAndIsDeletedFalse("profile-1")).thenReturn(Optional.of(sampleProfile));
        PaymentProfileResponseDto resDto = PaymentProfileResponseDto.builder().paymentProfileId("profile-1").build();
        when(mapper.toResponseDto(sampleProfile)).thenReturn(resDto);

        assertThat(service.getById("profile-1")).isEqualTo(resDto);
        assertThat(service.findById("profile-1")).isEqualTo(sampleProfile);

        when(repository.findByPaymentProfileIdAndIsDeletedFalse("invalid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("invalid")).isInstanceOf(PaymentProfileException.class);
    }

    @Test
    @DisplayName("getPrimaryProfile - Found vs Not Found")
    void getPrimaryProfile() {
        when(repository.findByCreator_Account_AccountIdAndIsPrimaryTrueAndIsDeletedFalseAndStatus(accountId, PaymentProfileStatus.VERIFIED))
                .thenReturn(Optional.of(sampleProfile));
        PaymentProfileResponseDto resDto = PaymentProfileResponseDto.builder().paymentProfileId("profile-1").build();
        when(mapper.toResponseDto(sampleProfile)).thenReturn(resDto);

        assertThat(service.getPrimaryProfile(accountId)).isEqualTo(resDto);

        when(repository.findByCreator_Account_AccountIdAndIsPrimaryTrueAndIsDeletedFalseAndStatus(accountId, PaymentProfileStatus.VERIFIED))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPrimaryProfile(accountId)).isInstanceOf(PaymentProfileException.class);
    }

    @Test
    @DisplayName("getOwnProfiles - Returns list of active profiles")
    void getOwnProfiles() {
        when(repository.findByCreator_Account_AccountIdAndIsDeletedFalse(accountId)).thenReturn(List.of(sampleProfile));
        when(mapper.toResponseDto(sampleProfile)).thenReturn(PaymentProfileResponseDto.builder().paymentProfileId("profile-1").build());

        List<PaymentProfileResponseDto> res = service.getOwnProfiles(accountId);

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("update - Setting as primary for VERIFIED or PENDING status")
    void update_PrimaryToggling() {
        when(repository.findByPaymentProfileIdAndIsDeletedFalse("profile-1")).thenReturn(Optional.of(sampleProfile));

        // Case 1: Existing status VERIFIED
        sampleProfile.setStatus(PaymentProfileStatus.VERIFIED);
        PaymentProfileRequestDto dto = PaymentProfileRequestDto.builder().isPrimary(true).build();
        when(repository.save(sampleProfile)).thenReturn(sampleProfile);
        when(mapper.toResponseDto(sampleProfile)).thenReturn(PaymentProfileResponseDto.builder().paymentProfileId("profile-1").build());

        service.update("profile-1", dto);

        verify(repository).unsetOtherPrimary("creator-1", "profile-1");
        assertThat(sampleProfile.getIsPrimary()).isTrue();

        // Case 2: Existing status PENDING with no existing verified primary
        sampleProfile.setIsPrimary(false);
        sampleProfile.setStatus(PaymentProfileStatus.PENDING);
        service.update("profile-1", dto);

        assertThat(sampleProfile.getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName("updateVerifiedStatus - Duplicate status exception vs VERIFIED status role update trigger")
    void updateVerifiedStatus() {
        when(repository.findByPaymentProfileIdAndIsDeletedFalse("profile-1")).thenReturn(Optional.of(sampleProfile));

        // Duplicate status exception
        PaymentProfileVerifiedDto dupDto = PaymentProfileVerifiedDto.builder().status(PaymentProfileStatus.PENDING).build();
        assertThatThrownBy(() -> service.updateVerifiedStatus("profile-1", dupDto))
                .isInstanceOf(PaymentProfileException.class)
                .hasMessageContaining("Status update bị trùng");

        // Change status to VERIFIED -> triggers creatorService.sendUpdateRoleRequest
        PaymentProfileVerifiedDto verifiedDto = PaymentProfileVerifiedDto.builder().status(PaymentProfileStatus.VERIFIED).verifiedNote("Approved").build();
        when(repository.save(sampleProfile)).thenReturn(sampleProfile);
        when(mapper.toResponseDto(sampleProfile)).thenReturn(PaymentProfileResponseDto.builder().paymentProfileId("profile-1").build());

        service.updateVerifiedStatus("profile-1", verifiedDto);

        assertThat(sampleProfile.getStatus()).isEqualTo(PaymentProfileStatus.VERIFIED);
        assertThat(sampleProfile.getVerifiedNote()).isEqualTo("Approved");
        verify(creatorService).sendUpdateRoleRequest(accountId);
    }

    @Test
    @DisplayName("delete - Primary verified profile throws exception vs non-primary soft delete")
    void delete() {
        when(repository.findByPaymentProfileIdAndIsDeletedFalse("profile-1")).thenReturn(Optional.of(sampleProfile));

        // Primary and VERIFIED -> Exception
        sampleProfile.setIsPrimary(true);
        sampleProfile.setStatus(PaymentProfileStatus.VERIFIED);
        assertThatThrownBy(() -> service.delete("profile-1"))
                .isInstanceOf(PaymentProfileException.class)
                .hasMessageContaining("Không thể xóa hồ sơ thanh toán chính duy nhất");

        // Non-primary -> Soft delete
        sampleProfile.setIsPrimary(false);
        service.delete("profile-1");

        assertThat(sampleProfile.getIsDeleted()).isTrue();
        verify(repository).save(sampleProfile);
    }

    @Test
    @DisplayName("list & getAllBankBins")
    void listAndGetAllBankBins() {
        PaymentProfileFilterRequestDto filterReq = PaymentProfileFilterRequestDto.builder()
                .page(1)
                .pageSize(10)
                .sortBy("bankCode")
                .sortDirection("ASC")
                .build();

        Page<PaymentProfile> page = new PageImpl<>(List.of(sampleProfile));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toResponseDto(sampleProfile)).thenReturn(PaymentProfileResponseDto.builder().paymentProfileId("profile-1").build());

        BasePageResponse<PaymentProfileResponseDto> res = service.list(filterReq);
        assertThat(res.getContent()).hasSize(1);

        List<BankBin> bankBins = service.getAllBankBins();
        assertThat(bankBins).isNotEmpty();
    }
}
