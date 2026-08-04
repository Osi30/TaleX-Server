package com.talex.server.services.interaction.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.request.WatchTimeRequest;
import com.talex.server.dtos.interaction.response.WatchSessionResponseDto;
import com.talex.server.entities.interaction.WatchSession;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.WatchSessionRepository;
import com.talex.server.services.interaction.IWatchSessionService;
import com.talex.server.services.series.EpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchSessionService implements IWatchSessionService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final WatchSessionRepository watchSessionRepository;
    private final EpisodeService episodeService;
    private static final String WATCH_PROGRESS_TOPIC = "watch-raw";

    @Async("interactionExecutor")
    @Override
    public void sendWatchHeartbeat(WatchTimeRequest request, UUID accountId, String ipAddress) {
        try {
            String finalAccountId = (accountId == null || accountId.toString().trim().isEmpty()) ? "" : accountId.toString();
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

    @Override
    @Transactional(readOnly = true)
    public Slice<WatchSessionResponseDto> getRecentWatchSessions(UUID accountId, Pageable pageable) {
        Slice<WatchSession> sessions = watchSessionRepository.findByAccountIdOrderByUpdatedAtDesc(accountId, pageable);

        return sessions.map(session -> WatchSessionResponseDto.builder()
                .id(session.getId())
                .episode(episodeService.toResponse(session.getEpisode()))
                .watchDuration(session.getWatchDuration())
                .heartbeatCount(session.getHeartbeatCount())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .currentPosition(session.getCurrentPosition())
                .updatedAt(session.getUpdatedAt())
                .build());
    }
}
