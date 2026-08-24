package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorTierRequestDto;
import com.talex.server.dtos.requests.filters.CreatorTierFilterRequestDto;
import com.talex.server.dtos.responses.creator.CreatorTierResponseDto;
import com.talex.server.entities.creator.CreatorTier;
import com.talex.server.exceptions.details.creator.CreatorTierException;
import com.talex.server.mappers.creator.CreatorTierMapper;
import com.talex.server.repositories.creator.CreatorTierRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatorTierServiceImpl Tests")
class CreatorTierServiceImplTest {

    @Mock
    private CreatorTierRepository repository;
    @Mock
    private CreatorTierMapper mapper;

    @InjectMocks
    private CreatorTierServiceImpl service;

    private CreatorTier defaultTier;
    private CreatorTier tierLevel1;
    private CreatorTier tierLevel2;

    @BeforeEach
    void setUp() {
        defaultTier = CreatorTier.builder()
                .creatorTierId("tier-0")
                .tierLevel(0)
                .isDefault(true)
                .isDeleted(false)
                .minFollowerRequired(0L)
                .minViewsRequired(0L)
                .minWatchTimeRequired(0.0)
                .build();

        tierLevel1 = CreatorTier.builder()
                .creatorTierId("tier-1")
                .tierLevel(1)
                .isDefault(false)
                .isDeleted(false)
                .minFollowerRequired(100L)
                .minViewsRequired(1000L)
                .minWatchTimeRequired(10.0)
                .build();

        tierLevel2 = CreatorTier.builder()
                .creatorTierId("tier-2")
                .tierLevel(2)
                .isDefault(false)
                .isDeleted(false)
                .minFollowerRequired(500L)
                .minViewsRequired(5000L)
                .minWatchTimeRequired(50.0)
                .build();
    }

    @Test
    @DisplayName("create - Setting isDefault resets requirements & unsets other defaults")
    void create_DefaultTier() {
        CreatorTierRequestDto dto = CreatorTierRequestDto.builder()
                .isDefault(true)
                .tierLevel(5) // should be reset to 0
                .minFollowerRequired(100L) // reset to 0
                .build();

        CreatorTier entity = CreatorTier.builder()
                .creatorTierId("new-default")
                .isDefault(true)
                .tierLevel(0)
                .minFollowerRequired(0L)
                .minViewsRequired(0L)
                .minWatchTimeRequired(0.0)
                .build();

        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.findAllByIsDeletedFalseOrderByTierLevelAsc()).thenReturn(List.of(tierLevel1));
        when(repository.save(entity)).thenReturn(entity);
        CreatorTierResponseDto resDto = CreatorTierResponseDto.builder().creatorTierId("new-default").build();
        when(mapper.toResponseDto(entity)).thenReturn(resDto);

        CreatorTierResponseDto result = service.create(dto);

        assertThat(result).isEqualTo(resDto);
        assertThat(dto.getTierLevel()).isEqualTo(0);
        assertThat(dto.getMinFollowerRequired()).isEqualTo(0L);
        verify(repository).unsetOtherDefaults("new-default");
    }

    @Test
    @DisplayName("validateMonotonicConstraints - Violations (duplicate level, non-monotonic followers/views/watchTime)")
    void validateMonotonicConstraints_Violations() {
        CreatorTierRequestDto dto = CreatorTierRequestDto.builder().build();

        // Duplicate level
        CreatorTier duplicateEntity = CreatorTier.builder().tierLevel(1).minFollowerRequired(1000L).minViewsRequired(10000L).minWatchTimeRequired(100.0).build();
        when(mapper.toEntity(dto)).thenReturn(duplicateEntity);
        when(repository.findAllByIsDeletedFalseOrderByTierLevelAsc()).thenReturn(List.of(tierLevel1));
        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(CreatorTierException.class).hasMessageContaining("bị trùng lặp");

        // Non-monotonic followers (tierLevel 2 has lower followers than tierLevel 1)
        CreatorTier badFollowers = CreatorTier.builder().tierLevel(2).minFollowerRequired(50L).minViewsRequired(5000L).minWatchTimeRequired(50.0).build();
        when(mapper.toEntity(dto)).thenReturn(badFollowers);
        when(repository.findAllByIsDeletedFalseOrderByTierLevelAsc()).thenReturn(List.of(tierLevel1));
        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(CreatorTierException.class).hasMessageContaining("Yêu cầu follower");

        // Non-monotonic views
        CreatorTier badViews = CreatorTier.builder().tierLevel(2).minFollowerRequired(500L).minViewsRequired(500L).minWatchTimeRequired(50.0).build();
        when(mapper.toEntity(dto)).thenReturn(badViews);
        when(repository.findAllByIsDeletedFalseOrderByTierLevelAsc()).thenReturn(List.of(tierLevel1));
        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(CreatorTierException.class).hasMessageContaining("Yêu cầu lượt xem");

        // Non-monotonic watch time
        CreatorTier badWatchTime = CreatorTier.builder().tierLevel(2).minFollowerRequired(500L).minViewsRequired(5000L).minWatchTimeRequired(5.0).build();
        when(mapper.toEntity(dto)).thenReturn(badWatchTime);
        when(repository.findAllByIsDeletedFalseOrderByTierLevelAsc()).thenReturn(List.of(tierLevel1));
        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(CreatorTierException.class).hasMessageContaining("Yêu cầu thời gian xem");
    }

    @Test
    @DisplayName("getById & findById - Success vs Not Found")
    void getByIdAndFindById() {
        when(repository.findByCreatorTierIdAndIsDeletedFalse("tier-1")).thenReturn(Optional.of(tierLevel1));
        CreatorTierResponseDto resDto = CreatorTierResponseDto.builder().creatorTierId("tier-1").build();
        when(mapper.toResponseDto(tierLevel1)).thenReturn(resDto);

        assertThat(service.getById("tier-1")).isEqualTo(resDto);
        assertThat(service.findById("tier-1")).isEqualTo(tierLevel1);

        when(repository.findByCreatorTierIdAndIsDeletedFalse("invalid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("invalid")).isInstanceOf(CreatorTierException.class);
    }

    @Test
    @DisplayName("getCurrentEligibleTier - Repository hit vs fallback to default tier")
    void getCurrentEligibleTier() {
        when(repository.findCurrentEligibleTier(150L, 2000L, 20.0)).thenReturn(Optional.of(tierLevel1));
        when(mapper.toResponseDto(tierLevel1)).thenReturn(CreatorTierResponseDto.builder().creatorTierId("tier-1").build());

        CreatorTierResponseDto res1 = service.getCurrentEligibleTier(150L, 2000L, 20.0);
        assertThat(res1.getCreatorTierId()).isEqualTo("tier-1");

        // Fallback to default
        when(repository.findCurrentEligibleTier(0L, 0L, 0.0)).thenReturn(Optional.empty());
        when(repository.findByIsDefaultTrueAndIsDeletedFalse()).thenReturn(Optional.of(defaultTier));
        when(mapper.toResponseDto(defaultTier)).thenReturn(CreatorTierResponseDto.builder().creatorTierId("tier-0").build());

        CreatorTierResponseDto res2 = service.getCurrentEligibleTier(null, null, null);
        assertThat(res2.getCreatorTierId()).isEqualTo("tier-0");
    }

    @Test
    @DisplayName("getNextTier - Next tier found vs highest tier achieved exception")
    void getNextTier() {
        when(repository.findFirstByTierLevelGreaterThanAndIsDeletedFalseOrderByTierLevelAsc(1)).thenReturn(Optional.of(tierLevel2));
        when(mapper.toResponseDto(tierLevel2)).thenReturn(CreatorTierResponseDto.builder().creatorTierId("tier-2").build());

        CreatorTierResponseDto res = service.getNextTier(1);
        assertThat(res.getCreatorTierId()).isEqualTo("tier-2");

        when(repository.findFirstByTierLevelGreaterThanAndIsDeletedFalseOrderByTierLevelAsc(2)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getNextTier(2)).isInstanceOf(CreatorTierException.class);
    }

    @Test
    @DisplayName("update - Updates entity and handles default tier toggling")
    void update() {
        when(repository.findByCreatorTierIdAndIsDeletedFalse("tier-1")).thenReturn(Optional.of(tierLevel1));
        CreatorTierRequestDto updateDto = CreatorTierRequestDto.builder().isDefault(true).build();

        doAnswer(inv -> {
            tierLevel1.setIsDefault(true);
            return null;
        }).when(mapper).updateEntity(any(), any());

        when(repository.findAllByIsDeletedFalseOrderByTierLevelAsc()).thenReturn(new ArrayList<>(List.of(tierLevel1)));
        when(repository.save(tierLevel1)).thenReturn(tierLevel1);
        when(mapper.toResponseDto(tierLevel1)).thenReturn(CreatorTierResponseDto.builder().creatorTierId("tier-1").build());

        service.update("tier-1", updateDto);

        verify(repository).unsetOtherDefaults("tier-1");
    }

    @Test
    @DisplayName("delete - Soft deletes entity")
    void delete() {
        when(repository.findByCreatorTierIdAndIsDeletedFalse("tier-1")).thenReturn(Optional.of(tierLevel1));

        service.delete("tier-1");

        assertThat(tierLevel1.getIsDeleted()).isTrue();
        verify(repository).save(tierLevel1);
    }

    @Test
    @DisplayName("list - Filtering, sorting, and pagination")
    void list() {
        CreatorTierFilterRequestDto filterReq = CreatorTierFilterRequestDto.builder()
                .page(1)
                .pageSize(10)
                .sortBy("tierLevel")
                .sortDirection("ASC")
                .build();

        Page<CreatorTier> page = new PageImpl<>(List.of(tierLevel1));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(mapper.toResponseDto(tierLevel1)).thenReturn(CreatorTierResponseDto.builder().creatorTierId("tier-1").build());

        BasePageResponse<CreatorTierResponseDto> res = service.list(filterReq);
        assertThat(res.getContent()).hasSize(1);
    }
}
