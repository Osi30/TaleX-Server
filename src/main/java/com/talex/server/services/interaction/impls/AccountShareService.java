package com.talex.server.services.interaction.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.request.ShareRequest;
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

@Service
@RequiredArgsConstructor
public class AccountShareService implements IAccountShareService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String SHARE_TOPIC = "talex-interaction.episode-shared";

    @Async("interactionExecutor")
    @Override
    public void shareEpisode(ShareRequest shareRequest) {
        try {
            if (ValidationUtils.isNullOrEmpty(shareRequest.getEpisodeId())) {
                throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR, "Tập phim không để trống!");
            }

            Map<String, Object> shareEvent = Map.of(
                    "account_id", shareRequest.getAccountId() == null ? "" : shareRequest.getAccountId().toString(),
                    "episode_id", shareRequest.getEpisodeId(),
                    "ip_address", shareRequest.getIpAddress(),
                    "timestamp", Instant.now().toEpochMilli()
            );

            String messagePayload = objectMapper.writeValueAsString(shareEvent);
            kafkaTemplate.send(SHARE_TOPIC, shareRequest.getEpisodeId(), messagePayload);

        } catch (Exception e) {
            throw new InteractionException(
                    InteractionErrorCode.KAFKA_PROCESSING_ERROR,
                    "Không thể ghi nhận lượt chia sẻ do sự cố hệ thống xếp hàng."
            );
        }
    }
}