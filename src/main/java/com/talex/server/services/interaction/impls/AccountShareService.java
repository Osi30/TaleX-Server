package com.talex.server.services.interaction.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.services.interaction.IAccountShareService;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountShareService implements IAccountShareService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String SHARE_TOPIC = "talex-interaction.episode-shared";

    @Async("interactionExecutor")
    @Override
    public void shareEpisode(UUID accountId, String episodeId) {
        try {
            if (ValidationUtils.isNullOrEmpty(episodeId)) {
                throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR, "Tập phim không để trống!");
            }

            Map<String, Object> shareEvent = Map.of(
                    "account_id", accountId.toString(),
                    "episode_id", episodeId,
                    "timestamp", Instant.now().toEpochMilli()
            );

            String messagePayload = objectMapper.writeValueAsString(shareEvent);
            kafkaTemplate.send(SHARE_TOPIC, episodeId, messagePayload);

        } catch (Exception e) {
            throw new InteractionException(
                    InteractionErrorCode.KAFKA_PROCESSING_ERROR,
                    "Không thể ghi nhận lượt chia sẻ do sự cố hệ thống xếp hàng."
            );
        }
    }
}