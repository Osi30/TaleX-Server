package com.talex.server.services.interaction.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.request.WatchTimeRequest;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.services.interaction.IWatchSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchSessionService implements IWatchSessionService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String WATCH_PROGRESS_TOPIC = "watch-raw";

    @Async("interactionExecutor")
    @Override
    public void sendWatchHeartbeat(WatchTimeRequest request, UUID accountId, String ipAddress) {
        try {
            String finalAccountId = (accountId == null || accountId.toString().trim().isEmpty()) ? "anonymous" : accountId.toString();
            String finalIpAddress = (ipAddress == null || ipAddress.trim().isEmpty()) ? "0.0.0.0" : ipAddress;

            Map<String, Object> watchEvent = Map.of(
                    "session_id", request.getSessionId(),
                    "episode_id", request.getEpisodeId(),
                    "current_position", request.getCurrentPosition(),
                    "heartbeat_value", request.getHeartbeatValue() != null ? request.getHeartbeatValue() : 0.0,
                    "event", request.getEvent(),
                    "account_id", finalAccountId,
                    "ip_address", finalIpAddress,
                    "timestamp", Instant.now().toEpochMilli()
            );

            String messagePayload = objectMapper.writeValueAsString(watchEvent);
            kafkaTemplate.send(WATCH_PROGRESS_TOPIC, request.getEpisodeId(), messagePayload);

        } catch (Exception e) {
            throw new InteractionException(
                    InteractionErrorCode.KAFKA_PROCESSING_ERROR,
                    "Không thể ghi nhận tiến trình xem do lỗi hệ thống hàng đợi: " + e.getMessage()
            );
        }
    }
}
