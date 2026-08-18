package com.talex.server.services.config.impls;

import com.talex.server.dtos.requests.config.AiPipelineConfigRequestDto;
import com.talex.server.dtos.responses.config.AiPipelineConfigResponseDto;
import com.talex.server.entities.config.AiPipelineConfig;
import com.talex.server.repositories.config.AiPipelineConfigRepository;
import com.talex.server.services.config.AiPipelineConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPipelineConfigServiceImpl implements AiPipelineConfigService {

    private final AiPipelineConfigRepository repository;

    @Override
    public AiPipelineConfigResponseDto getConfig() {
        return toDto(getOrCreateConfig());
    }

    @Override
    public AiPipelineConfigResponseDto updateConfig(AiPipelineConfigRequestDto request) {
        AiPipelineConfig config = getOrCreateConfig();
        config.setFingerprintSimilarityThreshold(request.getFingerprintSimilarityThreshold());
        config.setFingerprintClusterThreshold(request.getFingerprintClusterThreshold());
        config.setRekognitionConfidenceThreshold(request.getRekognitionConfidenceThreshold());
        config.setRekognitionViolenceConfidenceThreshold(request.getRekognitionViolenceConfidenceThreshold());
        config.setFingerprintImageTopK(request.getFingerprintImageTopK());
        config.setFingerprintVideoTopK(request.getFingerprintVideoTopK());
        config.setFingerprintMinMatchSeconds(request.getFingerprintMinMatchSeconds());
        config.setFingerprintMaxGapSeconds(request.getFingerprintMaxGapSeconds());
        config.setFingerprintFps(request.getFingerprintFps());
        config.setFingerprintMaxFrames(request.getFingerprintMaxFrames());
        config.setFingerprintMaxFileSizeMb(request.getFingerprintMaxFileSizeMb());
        config.setRekognitionMaxFrames(request.getRekognitionMaxFrames());
        config.setModerationFrameInterval(request.getModerationFrameInterval());

        AiPipelineConfig saved = repository.save(config);
        log.info("AI pipeline config updated: {}", saved.getConfigId());
        return toDto(saved);
    }

    // Bảng singleton: dùng row đầu tiên, tự tạo với default nếu chưa có row nào.
    private AiPipelineConfig getOrCreateConfig() {
        List<AiPipelineConfig> configs = repository.findAll();
        if (configs.isEmpty()) {
            return repository.save(AiPipelineConfig.builder().build());
        }
        return configs.get(0);
    }

    private AiPipelineConfigResponseDto toDto(AiPipelineConfig config) {
        return AiPipelineConfigResponseDto.builder()
                .configId(config.getConfigId())
                .fingerprintSimilarityThreshold(config.getFingerprintSimilarityThreshold())
                .fingerprintClusterThreshold(config.getFingerprintClusterThreshold())
                .rekognitionConfidenceThreshold(config.getRekognitionConfidenceThreshold())
                .rekognitionViolenceConfidenceThreshold(config.getRekognitionViolenceConfidenceThreshold())
                .fingerprintImageTopK(config.getFingerprintImageTopK())
                .fingerprintVideoTopK(config.getFingerprintVideoTopK())
                .fingerprintMinMatchSeconds(config.getFingerprintMinMatchSeconds())
                .fingerprintMaxGapSeconds(config.getFingerprintMaxGapSeconds())
                .fingerprintFps(config.getFingerprintFps())
                .fingerprintMaxFrames(config.getFingerprintMaxFrames())
                .fingerprintMaxFileSizeMb(config.getFingerprintMaxFileSizeMb())
                .rekognitionMaxFrames(config.getRekognitionMaxFrames())
                .moderationFrameInterval(config.getModerationFrameInterval())
                .build();
    }
}
