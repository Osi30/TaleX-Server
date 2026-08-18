package com.talex.server.services.config;

import com.talex.server.dtos.requests.config.AiPipelineConfigRequestDto;
import com.talex.server.dtos.responses.config.AiPipelineConfigResponseDto;
import com.talex.server.entities.config.AiPipelineConfig;
import com.talex.server.repositories.config.AiPipelineConfigRepository;
import com.talex.server.services.config.impls.AiPipelineConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiPipelineConfigService Tests")
class AiPipelineConfigServiceImplTest {

    @Mock
    private AiPipelineConfigRepository repository;

    @InjectMocks
    private AiPipelineConfigServiceImpl service;

    private AiPipelineConfig testConfig;
    private UUID testConfigId;

    @BeforeEach
    void setUp() {
        testConfigId = UUID.randomUUID();
        testConfig = AiPipelineConfig.builder()
                .configId(testConfigId)
                .fingerprintSimilarityThreshold(0.90)
                .fingerprintClusterThreshold(0.95)
                .rekognitionConfidenceThreshold(80.0)
                .rekognitionViolenceConfidenceThreshold(60.0)
                .build();
    }

    @Test
    @DisplayName("getConfig() trả về config hiện tại khi đã tồn tại")
    void testGetConfigWhenExists() {
        // Arrange
        List<AiPipelineConfig> configs = List.of(testConfig);
        when(repository.findAll()).thenReturn(configs);

        // Act
        AiPipelineConfigResponseDto response = service.getConfig();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getConfigId()).isEqualTo(testConfigId);
        assertThat(response.getFingerprintSimilarityThreshold()).isEqualTo(0.90);
        assertThat(response.getFingerprintClusterThreshold()).isEqualTo(0.95);
        assertThat(response.getRekognitionConfidenceThreshold()).isEqualTo(80.0);
        assertThat(response.getRekognitionViolenceConfidenceThreshold()).isEqualTo(60.0);
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("getConfig() tự tạo default config khi bảng rỗng (lazy-init)")
    void testGetConfigLazyInitWhenEmpty() {
        // Arrange: bảng rỗng
        when(repository.findAll()).thenReturn(new ArrayList<>());
        AiPipelineConfig newConfig = AiPipelineConfig.builder()
                .configId(testConfigId)
                .fingerprintSimilarityThreshold(0.90)
                .fingerprintClusterThreshold(0.95)
                .rekognitionConfidenceThreshold(80.0)
                .rekognitionViolenceConfidenceThreshold(60.0)
                .build();
        when(repository.save(any(AiPipelineConfig.class))).thenReturn(newConfig);

        // Act
        AiPipelineConfigResponseDto response = service.getConfig();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getFingerprintSimilarityThreshold()).isEqualTo(0.90);
        assertThat(response.getFingerprintClusterThreshold()).isEqualTo(0.95);
        assertThat(response.getRekognitionConfidenceThreshold()).isEqualTo(80.0);
        assertThat(response.getRekognitionViolenceConfidenceThreshold()).isEqualTo(60.0);
        verify(repository, times(1)).save(any(AiPipelineConfig.class));
    }

    @Test
    @DisplayName("updateConfig() cập nhật tất cả 4 ngưỡng")
    void testUpdateConfigSuccess() {
        // Arrange
        List<AiPipelineConfig> configs = List.of(testConfig);
        when(repository.findAll()).thenReturn(configs);

        AiPipelineConfigRequestDto request = new AiPipelineConfigRequestDto();
        request.setFingerprintSimilarityThreshold(0.85);
        request.setFingerprintClusterThreshold(0.92);
        request.setRekognitionConfidenceThreshold(75.0);
        request.setRekognitionViolenceConfidenceThreshold(55.0);

        AiPipelineConfig updated = AiPipelineConfig.builder()
                .configId(testConfigId)
                .fingerprintSimilarityThreshold(0.85)
                .fingerprintClusterThreshold(0.92)
                .rekognitionConfidenceThreshold(75.0)
                .rekognitionViolenceConfidenceThreshold(55.0)
                .build();
        when(repository.save(any(AiPipelineConfig.class))).thenReturn(updated);

        // Act
        AiPipelineConfigResponseDto response = service.updateConfig(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getFingerprintSimilarityThreshold()).isEqualTo(0.85);
        assertThat(response.getFingerprintClusterThreshold()).isEqualTo(0.92);
        assertThat(response.getRekognitionConfidenceThreshold()).isEqualTo(75.0);
        assertThat(response.getRekognitionViolenceConfidenceThreshold()).isEqualTo(55.0);
        verify(repository, times(1)).save(any(AiPipelineConfig.class));
    }

    @Test
    @DisplayName("updateConfig() tự tạo default config nếu chưa có, rồi update")
    void testUpdateConfigLazyInitThenUpdate() {
        // Arrange: bảng rỗng lần đầu (get), sau đó đã có config
        when(repository.findAll()).thenReturn(new ArrayList<>());

        AiPipelineConfig defaultConfig = AiPipelineConfig.builder()
                .configId(testConfigId)
                .fingerprintSimilarityThreshold(0.90)
                .fingerprintClusterThreshold(0.95)
                .rekognitionConfidenceThreshold(80.0)
                .rekognitionViolenceConfidenceThreshold(60.0)
                .build();
        when(repository.save(any(AiPipelineConfig.class))).thenReturn(defaultConfig);

        AiPipelineConfigRequestDto request = new AiPipelineConfigRequestDto();
        request.setFingerprintSimilarityThreshold(0.88);
        request.setFingerprintClusterThreshold(0.93);
        request.setRekognitionConfidenceThreshold(78.0);
        request.setRekognitionViolenceConfidenceThreshold(58.0);

        // Act
        AiPipelineConfigResponseDto response = service.updateConfig(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getFingerprintSimilarityThreshold()).isEqualTo(0.88);
        verify(repository, times(2)).save(any(AiPipelineConfig.class));
    }

    @Test
    @DisplayName("updateConfig() đặt giá trị biên (0.0 và 1.0 cho similarity, 0-100 cho Rekognition)")
    void testUpdateConfigBoundaryValues() {
        // Arrange
        List<AiPipelineConfig> configs = List.of(testConfig);
        when(repository.findAll()).thenReturn(configs);

        AiPipelineConfigRequestDto request = new AiPipelineConfigRequestDto();
        request.setFingerprintSimilarityThreshold(0.0);
        request.setFingerprintClusterThreshold(1.0);
        request.setRekognitionConfidenceThreshold(0.0);
        request.setRekognitionViolenceConfidenceThreshold(100.0);

        AiPipelineConfig updated = AiPipelineConfig.builder()
                .configId(testConfigId)
                .fingerprintSimilarityThreshold(0.0)
                .fingerprintClusterThreshold(1.0)
                .rekognitionConfidenceThreshold(0.0)
                .rekognitionViolenceConfidenceThreshold(100.0)
                .build();
        when(repository.save(any(AiPipelineConfig.class))).thenReturn(updated);

        // Act
        AiPipelineConfigResponseDto response = service.updateConfig(request);

        // Assert
        assertThat(response.getFingerprintSimilarityThreshold()).isEqualTo(0.0);
        assertThat(response.getFingerprintClusterThreshold()).isEqualTo(1.0);
        assertThat(response.getRekognitionConfidenceThreshold()).isEqualTo(0.0);
        assertThat(response.getRekognitionViolenceConfidenceThreshold()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getOrCreateConfig luôn trả về row thứ nhất khi có nhiều row (singleton)")
    void testGetOrCreateConfigSingletonBehavior() {
        // Arrange
        UUID config1Id = UUID.randomUUID();
        UUID config2Id = UUID.randomUUID();

        AiPipelineConfig config1 = AiPipelineConfig.builder()
                .configId(config1Id)
                .fingerprintSimilarityThreshold(0.90)
                .fingerprintClusterThreshold(0.95)
                .rekognitionConfidenceThreshold(80.0)
                .rekognitionViolenceConfidenceThreshold(60.0)
                .build();

        AiPipelineConfig config2 = AiPipelineConfig.builder()
                .configId(config2Id)
                .fingerprintSimilarityThreshold(0.85)
                .fingerprintClusterThreshold(0.90)
                .rekognitionConfidenceThreshold(75.0)
                .rekognitionViolenceConfidenceThreshold(55.0)
                .build();

        List<AiPipelineConfig> configs = List.of(config1, config2);
        when(repository.findAll()).thenReturn(configs);

        // Act
        AiPipelineConfigResponseDto response = service.getConfig();

        // Assert — phải là config1 (first)
        assertThat(response.getConfigId()).isEqualTo(config1Id);
        assertThat(response.getFingerprintSimilarityThreshold()).isEqualTo(0.90);
    }
}
