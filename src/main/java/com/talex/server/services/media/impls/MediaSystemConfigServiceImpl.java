package com.talex.server.services.media.impls;

import com.talex.server.dtos.requests.media.MediaSystemConfigRequestDto;
import com.talex.server.dtos.responses.media.MediaSystemConfigResponseDto;
import com.talex.server.entities.media.MediaSystemConfig;
import com.talex.server.repositories.media.MediaSystemConfigRepository;
import com.talex.server.services.media.MediaSystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaSystemConfigServiceImpl implements MediaSystemConfigService {

    private final MediaSystemConfigRepository repository;

    @Override
    public MediaSystemConfigResponseDto getConfig() {
        MediaSystemConfig config = getOrCreateConfig();
        return toDto(config);
    }

    @Override
    public MediaSystemConfigResponseDto updateConfig(MediaSystemConfigRequestDto request) {
        MediaSystemConfig config = getOrCreateConfig();
        config.setMaxComicImages(request.getMaxComicImages());
        config.setMaxComicImageSizeMb(request.getMaxComicImageSizeMb());
        config.setMaxVideoSizeMb(request.getMaxVideoSizeMb());
        
        MediaSystemConfig savedConfig = repository.save(config);
        log.info("Media system config updated: {}", savedConfig.getConfigId());
        return toDto(savedConfig);
    }

    private MediaSystemConfig getOrCreateConfig() {
        List<MediaSystemConfig> configs = repository.findAll();
        if (configs.isEmpty()) {
            MediaSystemConfig newConfig = MediaSystemConfig.builder()
                    .maxComicImages(20)
                    .maxComicImageSizeMb(3.0)
                    .maxVideoSizeMb(30.0)
                    .build();
            return repository.save(newConfig);
        }
        return configs.get(0);
    }

    private MediaSystemConfigResponseDto toDto(MediaSystemConfig config) {
        return MediaSystemConfigResponseDto.builder()
                .configId(config.getConfigId())
                .maxComicImages(config.getMaxComicImages())
                .maxComicImageSizeMb(config.getMaxComicImageSizeMb())
                .maxVideoSizeMb(config.getMaxVideoSizeMb())
                .build();
    }
}
