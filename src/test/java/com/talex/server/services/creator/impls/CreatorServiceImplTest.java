package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.analytic.CreatorLogResponseDto;
import com.talex.server.dtos.requests.creator.CreatorRegisterDto;
import com.talex.server.dtos.requests.filters.CreatorFilterRequestDto;
import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.dtos.responses.creator.TermsVersionResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.CreatorLog;
import com.talex.server.enums.AccountStatus;
import com.talex.server.enums.TermsType;
import com.talex.server.exceptions.details.creator.CreatorException;
import com.talex.server.mappers.creator.CreatorMapper;
import com.talex.server.records.CreatorVerificationStatus;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.creator.CreatorLogRepository;
import com.talex.server.repositories.creator.CreatorRepository;
import com.talex.server.services.creator.CreatorIdentityService;
import com.talex.server.services.terms.TermsLogService;
import com.talex.server.services.terms.TermsVersionService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatorServiceImpl Tests")
class CreatorServiceImplTest {

    @Mock
    private TermsVersionService termsVersionService;
    @Mock
    private TermsLogService creatorTermsLogService;
    @Mock
    private CreatorIdentityService creatorIdentityService;
    @Mock
    private CreatorRepository creatorRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CreatorLogRepository creatorLogRepository;
    @Mock
    private CreatorMapper creatorMapper;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private CreatorServiceImpl service;

    private UUID accountId;
    private Account sampleAccount;
    private Creator sampleCreator;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        sampleAccount = Account.builder().accountId(accountId).status(AccountStatus.ACTIVE).build();
        sampleCreator = Creator.builder().creatorId("creator-1").account(sampleAccount).build();
    }

    @Test
    @DisplayName("createCreator - Success vs Account Not Found")
    void createCreator() {
        CreatorRegisterDto dto = CreatorRegisterDto.builder()
                .accountId(accountId)
                .termsId("term-1")
                .build();

        when(accountRepository.findByAccountIdAndStatus(accountId, AccountStatus.ACTIVE)).thenReturn(Optional.of(sampleAccount));
        when(creatorRepository.save(any(Creator.class))).thenReturn(sampleCreator);
        CreatorResponseDto resDto = CreatorResponseDto.builder().creatorId("creator-1").build();
        when(creatorMapper.toResponseDto(sampleCreator)).thenReturn(resDto);

        CreatorResponseDto result = service.createCreator(dto);

        assertThat(result).isEqualTo(resDto);
        verify(creatorTermsLogService).create(eq(sampleAccount), any());

        // Account not found
        when(accountRepository.findByAccountIdAndStatus(accountId, AccountStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createCreator(dto))
                .isInstanceOf(CreatorException.class);
    }

    @Test
    @DisplayName("verifyCreator - Invalid termsId vs Success")
    void verifyCreator() {
        CreatorRegisterDto dtoInvalid = CreatorRegisterDto.builder().accountId(accountId).termsId("").build();
        when(creatorRepository.findByAccount_AccountId(accountId)).thenReturn(Optional.of(sampleCreator));

        assertThatThrownBy(() -> service.verifyCreator(dtoInvalid))
                .isInstanceOf(CreatorException.class);

        CreatorRegisterDto dtoValid = CreatorRegisterDto.builder().accountId(accountId).termsId("term-1").build();

        String res = service.verifyCreator(dtoValid);

        assertThat(res).isEqualTo("Xác thực thành công");
        assertThat(sampleCreator.getIsVerified()).isTrue();
        verify(creatorTermsLogService).create(eq(accountId), any());
        verify(creatorIdentityService).create(sampleCreator);
        verify(creatorRepository).save(sampleCreator);
    }

    @Test
    @DisplayName("checkAndGetVerificationStatus - Not Found vs Not Verified vs Verified")
    void checkAndGetVerificationStatus() {
        // Not Found
        when(creatorRepository.getVerificationStatusByAccountId(accountId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.checkAndGetVerificationStatus(accountId))
                .isInstanceOf(CreatorException.class);

        // Not verified
        CreatorVerificationStatus unverifiedStatus = new CreatorVerificationStatus(false, false, null, null, null, null, null, null, null);
        when(creatorRepository.getVerificationStatusByAccountId(accountId)).thenReturn(Optional.of(unverifiedStatus));
        assertThatThrownBy(() -> service.checkAndGetVerificationStatus(accountId))
                .isInstanceOf(CreatorException.class);

        // Verified
        CreatorVerificationStatus verifiedStatus = new CreatorVerificationStatus(true, true, null, null, null, null, null, null, null);
        when(creatorRepository.getVerificationStatusByAccountId(accountId)).thenReturn(Optional.of(verifiedStatus));
        CreatorVerificationStatus res = service.checkAndGetVerificationStatus(accountId);
        assertThat(res.isCreatorVerified()).isTrue();
    }

    @Test
    @DisplayName("sendUpdateRoleRequest - Null accountId vs valid accountId")
    void sendUpdateRoleRequest() {
        service.sendUpdateRoleRequest(null);
        verify(kafkaTemplate, never()).send(anyString(), anyString());

        service.sendUpdateRoleRequest(accountId);
        verify(kafkaTemplate).send("request-to-update-account", accountId.toString());
    }

    @Test
    @DisplayName("updateBalance - Accumulates current and total balance")
    void updateBalance() {
        when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(sampleCreator));

        service.updateBalance("creator-1", BigDecimal.valueOf(50000));

        assertThat(sampleCreator.getCurrentBalance()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(sampleCreator.getTotalBalance()).isEqualTo(BigDecimal.valueOf(50000));
        verify(creatorRepository).save(sampleCreator);

        // Second update
        service.updateBalance("creator-1", BigDecimal.valueOf(20000));
        assertThat(sampleCreator.getCurrentBalance()).isEqualTo(BigDecimal.valueOf(70000));
        assertThat(sampleCreator.getTotalBalance()).isEqualTo(BigDecimal.valueOf(70000));
    }

    @Test
    @DisplayName("getById & getEntityById - Success vs Not Found")
    void getByIdAndEntityById() {
        when(creatorRepository.findById("creator-1")).thenReturn(Optional.of(sampleCreator));
        CreatorResponseDto resDto = CreatorResponseDto.builder().creatorId("creator-1").build();
        when(creatorMapper.toResponseDto(sampleCreator)).thenReturn(resDto);

        assertThat(service.getById("creator-1")).isEqualTo(resDto);
        assertThat(service.getEntityById("creator-1")).isEqualTo(sampleCreator);

        when(creatorRepository.findById("invalid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("invalid")).isInstanceOf(CreatorException.class);
    }

    @Test
    @DisplayName("getByAccount - Account not registered vs registered with/without latest terms")
    void getByAccount() {
        when(creatorRepository.findByAccount_AccountId(accountId)).thenReturn(Optional.of(sampleCreator));

        TermsVersionResponseDto activeTerm = TermsVersionResponseDto.builder().id("term-v2").build();
        when(termsVersionService.getActiveByType(TermsType.CREATOR)).thenReturn(activeTerm);

        // Case 1: Terms accepted
        when(creatorTermsLogService.existsByAccountAndTerm(accountId, "term-v2")).thenReturn(true);
        CreatorResponseDto dto1 = CreatorResponseDto.builder().creatorId("creator-1").build();
        when(creatorMapper.toResponseDto(sampleCreator)).thenReturn(dto1);

        CreatorResponseDto res1 = service.getByAccount(accountId);
        assertThat(res1.getIsAcceptedLatestTerms()).isTrue();

        // Case 2: Terms not accepted
        when(creatorTermsLogService.existsByAccountAndTerm(accountId, "term-v2")).thenReturn(false);
        CreatorResponseDto dto2 = CreatorResponseDto.builder().creatorId("creator-1").build();
        when(creatorMapper.toResponseDto(sampleCreator)).thenReturn(dto2);

        CreatorResponseDto res2 = service.getByAccount(accountId);
        assertThat(res2.getIsAcceptedLatestTerms()).isFalse();
        assertThat(res2.getTermsVersion()).isEqualTo(activeTerm);
    }

    @Test
    @DisplayName("getIdByAccountId - Success vs Not Found")
    void getIdByAccountId() {
        when(creatorRepository.findCreatorIdByAccountId(accountId)).thenReturn(Optional.of("creator-1"));
        assertThat(service.getIdByAccountId(accountId)).isEqualTo("creator-1");

        when(creatorRepository.findCreatorIdByAccountId(accountId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getIdByAccountId(accountId)).isInstanceOf(CreatorException.class);
    }

    @Test
    @DisplayName("filterCreators - Sorting, searchKey unsorted, and pagination")
    void filterCreators() {
        CreatorFilterRequestDto filterReq = CreatorFilterRequestDto.builder()
                .page(1)
                .pageSize(10)
                .searchKey("John")
                .build();

        Page<Creator> page = new PageImpl<>(List.of(sampleCreator));
        when(creatorRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(creatorMapper.toResponseDto(sampleCreator)).thenReturn(CreatorResponseDto.builder().creatorId("creator-1").build());

        BasePageResponse<CreatorResponseDto> response = service.filterCreators(filterReq);
        assertThat(response.getContent()).hasSize(1);

        // With sortBy
        filterReq.setSearchKey(null);
        filterReq.setSortBy("nickname");
        filterReq.setSortDirection("ASC");
        service.filterCreators(filterReq);
        verify(creatorRepository, times(2)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getCreatorLogs - Fallback for null dates")
    void getCreatorLogs() {
        CreatorLog log1 = CreatorLog.builder()
                .creatorLogId("log-1")
                .hourBucket(LocalDateTime.now())
                .follows(5L)
                .build();

        when(creatorLogRepository.findByAccount_AccountIdAndHourBucketBetweenOrderByHourBucketAsc(
                eq(accountId), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(List.of(log1));

        List<CreatorLogResponseDto> logs = service.getCreatorLogs(accountId, null, null);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getCreatorLogId()).isEqualTo("log-1");
    }
}
