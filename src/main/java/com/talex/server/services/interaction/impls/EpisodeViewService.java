package com.talex.server.services.interaction.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.services.interaction.IEpisodeViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpisodeViewService implements IEpisodeViewService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String VIEW_TOPIC = "talex-interaction.episode-viewed";

    @Async("interactionExecutor")
    @Override
    public void viewEpisode(String ipAddress, String episodeId) {
        try {
            // Đóng gói JSON phẳng tối ưu băng thông mạng
            Map<String, Object> viewEvent = Map.of(
                    "ip_address", ipAddress != null ? ipAddress : "0.0.0.0",
                    "episode_id", episodeId,
                    "timestamp", Instant.now().toEpochMilli()
            );

            String messagePayload = objectMapper.writeValueAsString(viewEvent);
            kafkaTemplate.send(VIEW_TOPIC, episodeId, messagePayload);

        } catch (Exception e) {
            log.error("Lỗi khi gửi sự kiện View lên Kafka: ", e);
            throw new InteractionException(
                    InteractionErrorCode.KAFKA_PROCESSING_ERROR,
                    "Không thể ghi nhận lượt xem do sự cố hệ thống." + e.getMessage()
            );
        }
    }
}