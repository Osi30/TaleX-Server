package com.talex.server.controllers.config;

import com.talex.server.dtos.requests.config.AiPipelineConfigRequestDto;
import com.talex.server.dtos.responses.config.AiPipelineConfigResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("AiPipelineConfigController Validation Tests")
class AiPipelineConfigControllerTest {

    @Autowired
    private Validator validator;

    private AiPipelineConfigRequestDto validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new AiPipelineConfigRequestDto();
        validRequest.setFingerprintSimilarityThreshold(0.90);
        validRequest.setFingerprintClusterThreshold(0.95);
        validRequest.setRekognitionConfidenceThreshold(80.0);
        validRequest.setRekognitionViolenceConfidenceThreshold(60.0);
    }

    @Test
    @DisplayName("Valid request passes validation")
    void testValidRequest() {
        // Act
        Set<ConstraintViolation<AiPipelineConfigRequestDto>> violations = validator.validate(validRequest);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Validation reject similarity > 1.0")
    void testValidationSimilarityTooHigh() {
        // Arrange
        validRequest.setFingerprintSimilarityThreshold(1.5);

        // Act
        Set<ConstraintViolation<AiPipelineConfigRequestDto>> violations = validator.validate(validRequest);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fingerprintSimilarityThreshold"));
    }

    @Test
    @DisplayName("Validation reject similarity < 0.0")
    void testValidationSimilarityTooLow() {
        // Arrange
        validRequest.setFingerprintSimilarityThreshold(-0.1);

        // Act
        Set<ConstraintViolation<AiPipelineConfigRequestDto>> violations = validator.validate(validRequest);

        // Assert
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Validation reject cluster > 1.0")
    void testValidationClusterTooHigh() {
        // Arrange
        validRequest.setFingerprintClusterThreshold(1.01);

        // Act
        Set<ConstraintViolation<AiPipelineConfigRequestDto>> violations = validator.validate(validRequest);

        // Assert
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Validation reject Rekognition confidence > 100")
    void testValidationRekognitionTooHigh() {
        // Arrange
        validRequest.setRekognitionConfidenceThreshold(101.0);

        // Act
        Set<ConstraintViolation<AiPipelineConfigRequestDto>> violations = validator.validate(validRequest);

        // Assert
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Validation reject violence confidence < 0")
    void testValidationViolenceTooLow() {
        // Arrange
        validRequest.setRekognitionViolenceConfidenceThreshold(-5.0);

        // Act
        Set<ConstraintViolation<AiPipelineConfigRequestDto>> violations = validator.validate(validRequest);

        // Assert
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Validation accept boundary values 0.0 and 1.0")
    void testValidationBoundaryAccepted() {
        // Arrange
        validRequest.setFingerprintSimilarityThreshold(0.0);
        validRequest.setFingerprintClusterThreshold(1.0);
        validRequest.setRekognitionConfidenceThreshold(0.0);
        validRequest.setRekognitionViolenceConfidenceThreshold(100.0);

        // Act
        Set<ConstraintViolation<AiPipelineConfigRequestDto>> violations = validator.validate(validRequest);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Validation reject null values")
    void testValidationNullValue() {
        // Arrange
        validRequest.setFingerprintSimilarityThreshold(null);

        // Act
        Set<ConstraintViolation<AiPipelineConfigRequestDto>> violations = validator.validate(validRequest);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fingerprintSimilarityThreshold"));
    }

    @Test
    @DisplayName("DTO dto response shape correct")
    void testResponseDtoShape() {
        // Arrange
        UUID configId = UUID.randomUUID();
        AiPipelineConfigResponseDto response = AiPipelineConfigResponseDto.builder()
                .configId(configId)
                .fingerprintSimilarityThreshold(0.90)
                .fingerprintClusterThreshold(0.95)
                .rekognitionConfidenceThreshold(80.0)
                .rekognitionViolenceConfidenceThreshold(60.0)
                .build();

        // Assert
        assertThat(response.getConfigId()).isEqualTo(configId);
        assertThat(response.getFingerprintSimilarityThreshold()).isEqualTo(0.90);
        assertThat(response.getFingerprintClusterThreshold()).isEqualTo(0.95);
        assertThat(response.getRekognitionConfidenceThreshold()).isEqualTo(80.0);
        assertThat(response.getRekognitionViolenceConfidenceThreshold()).isEqualTo(60.0);
    }
}
