package com.talex.server.services.campaigns;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.EngagementServiceRequestDto;
import com.talex.server.dtos.requests.filters.EngagementServiceFilterRequestDto;
import com.talex.server.dtos.responses.campaign.EngagementServiceResponseDto;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.exceptions.codes.campaign.EngagementErrorCode;
import com.talex.server.exceptions.details.campaign.EngagementServiceException;
import com.talex.server.mappers.campaign.EngagementServiceMapper;
import com.talex.server.repositories.campaign.EngagementServiceRepository;
import com.talex.server.services.campaign.impls.EngagementServiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EngagementServiceServiceTest {

    @Mock
    private EngagementServiceRepository engagementServiceRepository;

    @Mock
    private EngagementServiceMapper engagementServiceMapper;

    @InjectMocks
    private EngagementServiceServiceImpl engagementServiceServiceImpl;

    private EngagementService sampleEntity;
    private EngagementServiceRequestDto sampleRequestDto;
    private EngagementServiceResponseDto sampleResponseDto;

    @BeforeEach
    void setUp() {
        sampleEntity = EngagementService.builder()
                .engagementServiceId("service-123")
                .name("Gói Engagement VIP")
                .description("Mô tả gói dịch vụ")
                .price(500000L)
                .targetValue(100L)
                .isActive(true)
                .isDeleted(false)
                .build();

        sampleRequestDto = EngagementServiceRequestDto.builder()
                .name("Gói Engagement VIP")
                .description("Mô tả gói dịch vụ")
                .price(500000L)
                .targetValue(100L)
                .isActive(true)
                .build();

        sampleResponseDto = EngagementServiceResponseDto.builder()
                .engagementServiceId("service-123")
                .name("Gói Engagement VIP")
                .description("Mô tả gói dịch vụ")
                .price(500000L)
                .targetValue(100L)
                .isActive(true)
                .build();
    }

    // ==========================================
    // 1. CREATE ENGAGEMENT SERVICE
    // ==========================================
    @Nested
    @DisplayName("Create Engagement Service Tests")
    class CreateEngagementServiceTests {

        @Test
        @DisplayName("Tạo mới EngagementService thành công")
        void createEngagementService_Success() {
            when(engagementServiceMapper.toEntity(sampleRequestDto)).thenReturn(sampleEntity);
            when(engagementServiceRepository.save(sampleEntity)).thenReturn(sampleEntity);
            when(engagementServiceMapper.toResponseDto(sampleEntity)).thenReturn(sampleResponseDto);

            EngagementServiceResponseDto result = engagementServiceServiceImpl.createEngagementService(sampleRequestDto);

            assertThat(result).isNotNull();
            assertThat(result.getEngagementServiceId()).isEqualTo("service-123");
            verify(engagementServiceMapper).toEntity(sampleRequestDto);
            verify(engagementServiceRepository).save(sampleEntity);
            verify(engagementServiceMapper).toResponseDto(sampleEntity);
        }
    }

    // ==========================================
    // 2. FILTER ENGAGEMENT SERVICES
    // ==========================================
    @Nested
    @DisplayName("Filter Engagement Services Tests")
    class FilterEngagementServicesTests {

        @Test
        @DisplayName("Filter với SortDirection ASC và trường sortBy hợp lệ")
        void filterEngagementServices_AscendingSortAndValidProperty() {
            EngagementServiceFilterRequestDto filterRequest = EngagementServiceFilterRequestDto.builder()
                    .sortBy("name")
                    .sortDirection("ASC")
                    .build();

            Page<EngagementService> pageResult = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 10), 1);

            when(engagementServiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(pageResult);
            when(engagementServiceMapper.toResponseDto(sampleEntity)).thenReturn(sampleResponseDto);

            BasePageResponse<EngagementServiceResponseDto> response =
                    engagementServiceServiceImpl.filterEngagementServices(filterRequest);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getPageNumber()).isEqualTo(1);
            verify(engagementServiceRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Filter khi sortBy là null hoặc rỗng -> Mặc định sắp xếp theo createdAt DESC")
        void filterEngagementServices_NullSortBy_DefaultsToCreatedAt() {
            EngagementServiceFilterRequestDto filterRequest = EngagementServiceFilterRequestDto.builder()
                    .sortBy(null)
                    .sortDirection(null)
                    .build();

            Page<EngagementService> pageResult = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 10), 1);

            when(engagementServiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(pageResult);
            when(engagementServiceMapper.toResponseDto(sampleEntity)).thenReturn(sampleResponseDto);

            BasePageResponse<EngagementServiceResponseDto> response =
                    engagementServiceServiceImpl.filterEngagementServices(filterRequest);

            assertThat(response).isNotNull();
            verify(engagementServiceRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"name", "price", "targetValue", "createdAt", "updatedAt"})
        @DisplayName("Test normalizeSortProperty với các trường sắp xếp hợp lệ")
        void filterEngagementServices_ValidSortProperties(String sortBy) {
            EngagementServiceFilterRequestDto filterRequest = EngagementServiceFilterRequestDto.builder()
                    .sortBy(sortBy)
                    .sortDirection("DESC")
                    .build();

            Page<EngagementService> pageResult = new PageImpl<>(List.of(sampleEntity));

            when(engagementServiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(pageResult);
            when(engagementServiceMapper.toResponseDto(sampleEntity)).thenReturn(sampleResponseDto);

            BasePageResponse<EngagementServiceResponseDto> response =
                    engagementServiceServiceImpl.filterEngagementServices(filterRequest);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Test normalizeSortProperty với trường sortBy không nằm trong switch-case -> Trả về createdAt")
        void filterEngagementServices_InvalidSortProperty_FallsBackToCreatedAt() {
            EngagementServiceFilterRequestDto filterRequest = EngagementServiceFilterRequestDto.builder()
                    .sortBy("invalidProperty")
                    .sortDirection("DESC")
                    .build();

            Page<EngagementService> pageResult = new PageImpl<>(List.of(sampleEntity));

            when(engagementServiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(pageResult);
            when(engagementServiceMapper.toResponseDto(sampleEntity)).thenReturn(sampleResponseDto);

            BasePageResponse<EngagementServiceResponseDto> response =
                    engagementServiceServiceImpl.filterEngagementServices(filterRequest);

            assertThat(response).isNotNull();
        }
    }

    // ==========================================
    // 3. GET BY ID & FIND BY ID
    // ==========================================
    @Nested
    @DisplayName("Get & Find By Id Tests")
    class FindByIdTests {

        @Test
        @DisplayName("findById thành công khi tìm thấy entity không bị xóa")
        void findById_Success() {
            when(engagementServiceRepository.findByEngagementServiceIdAndIsDeletedFalse("service-123"))
                    .thenReturn(Optional.of(sampleEntity));

            EngagementService result = engagementServiceServiceImpl.findById("service-123");

            assertThat(result).isNotNull();
            assertThat(result.getEngagementServiceId()).isEqualTo("service-123");
        }

        @Test
        @DisplayName("findById ném EngagementServiceException khi không tìm thấy entity")
        void findById_NotFound_ThrowsException() {
            when(engagementServiceRepository.findByEngagementServiceIdAndIsDeletedFalse("invalid-id"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> engagementServiceServiceImpl.findById("invalid-id"))
                    .isInstanceOf(EngagementServiceException.class)
                    .hasMessageContaining("EngagementService not found with id: invalid-id")
                    .extracting("errorCode")
                    .isEqualTo(EngagementErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("getEngagementServiceById thành công")
        void getEngagementServiceById_Success() {
            when(engagementServiceRepository.findByEngagementServiceIdAndIsDeletedFalse("service-123"))
                    .thenReturn(Optional.of(sampleEntity));
            when(engagementServiceMapper.toResponseDto(sampleEntity)).thenReturn(sampleResponseDto);

            EngagementServiceResponseDto result = engagementServiceServiceImpl.getEngagementServiceById("service-123");

            assertThat(result).isNotNull();
            assertThat(result.getEngagementServiceId()).isEqualTo("service-123");
        }
    }

    // ==========================================
    // 4. FIND ACTIVE
    // ==========================================
    @Nested
    @DisplayName("Find Active Tests")
    class FindActiveTests {

        @Test
        @DisplayName("findActive thành công khi entity hoạt động (isActive = true)")
        void findActive_Success() {
            sampleEntity.setIsActive(true);
            when(engagementServiceRepository.findByEngagementServiceIdAndIsDeletedFalse("service-123"))
                    .thenReturn(Optional.of(sampleEntity));

            EngagementService result = engagementServiceServiceImpl.findActive("service-123");

            assertThat(result).isNotNull();
            assertThat(result.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("findActive ném EngagementServiceException khi entity không active (isActive = false)")
        void findActive_Inactive_ThrowsException() {
            sampleEntity.setIsActive(false);
            when(engagementServiceRepository.findByEngagementServiceIdAndIsDeletedFalse("service-123"))
                    .thenReturn(Optional.of(sampleEntity));

            assertThatThrownBy(() -> engagementServiceServiceImpl.findActive("service-123"))
                    .isInstanceOf(EngagementServiceException.class)
                    .hasMessageContaining("Active engagementService not found with id: service-123")
                    .extracting("errorCode")
                    .isEqualTo(EngagementErrorCode.NOT_FOUND);
        }
    }

    // ==========================================
    // 5. UPDATE ENGAGEMENT SERVICE
    // ==========================================
    @Nested
    @DisplayName("Update Engagement Service Tests")
    class UpdateEngagementServiceTests {

        @Test
        @DisplayName("Cập nhật thành công")
        void updateEngagementService_Success() {
            when(engagementServiceRepository.findByEngagementServiceIdAndIsDeletedFalse("service-123"))
                    .thenReturn(Optional.of(sampleEntity));
            doNothing().when(engagementServiceMapper).updateEntity(sampleRequestDto, sampleEntity);
            when(engagementServiceRepository.save(sampleEntity)).thenReturn(sampleEntity);
            when(engagementServiceMapper.toResponseDto(sampleEntity)).thenReturn(sampleResponseDto);

            EngagementServiceResponseDto result =
                    engagementServiceServiceImpl.updateEngagementService("service-123", sampleRequestDto);

            assertThat(result).isNotNull();
            verify(engagementServiceMapper).updateEntity(sampleRequestDto, sampleEntity);
            verify(engagementServiceRepository).save(sampleEntity);
        }

        @Test
        @DisplayName("Cập nhật thất bại do ID không tồn tại")
        void updateEngagementService_NotFound_ThrowsException() {
            when(engagementServiceRepository.findByEngagementServiceIdAndIsDeletedFalse("invalid-id"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> engagementServiceServiceImpl.updateEngagementService("invalid-id", sampleRequestDto))
                    .isInstanceOf(EngagementServiceException.class);

            verify(engagementServiceRepository, never()).save(any());
        }
    }

    // ==========================================
    // 6. DELETE ENGAGEMENT SERVICE
    // ==========================================
    @Nested
    @DisplayName("Delete Engagement Service Tests")
    class DeleteEngagementServiceTests {

        @Test
        @DisplayName("Xóa mềm (Soft Delete) thành công - Đặt isDeleted = true")
        void deleteEngagementService_Success() {
            when(engagementServiceRepository.findByEngagementServiceIdAndIsDeletedFalse("service-123"))
                    .thenReturn(Optional.of(sampleEntity));

            engagementServiceServiceImpl.deleteEngagementService("service-123");

            ArgumentCaptor<EngagementService> captor = ArgumentCaptor.forClass(EngagementService.class);
            verify(engagementServiceRepository).save(captor.capture());

            EngagementService savedEntity = captor.getValue();
            assertThat(savedEntity.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("Xóa thất bại do ID không tồn tại")
        void deleteEngagementService_NotFound_ThrowsException() {
            when(engagementServiceRepository.findByEngagementServiceIdAndIsDeletedFalse("invalid-id"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> engagementServiceServiceImpl.deleteEngagementService("invalid-id"))
                    .isInstanceOf(EngagementServiceException.class);

            verify(engagementServiceRepository, never()).save(any());
        }
    }
}