package com.talex.server.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.entities.report.Penalty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PenaltyWorker {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "penalty-event-topic",
            groupId = "penalty-worker-group"
    )
    @Transactional
    public void processPenaltyEvent(String messagePayload) {
        try {
            // Cast JSON payload sang Object Penalty
            Penalty penalty = objectMapper.readValue(messagePayload, Penalty.class);

            if (penalty == null) {
                return;
            }

            // Log thông tin Penalty nhận được
            log.info("[PenaltyWorker] Nhận sự kiện Penalty thành công | PenaltyId: {} | TargetUserId: {} | Level: {} | TargetType: {} | TargetId: {} | Status: {} | ExpiresAt: {}",
                    penalty.getPenaltyId(),
                    penalty.getTargetUserId(),
                    penalty.getLevel(),
                    penalty.getTargetType(),
                    penalty.getTargetId(),
                    penalty.getStatus());

            // TODO: Xử lý logic nghiệp vụ bổ sung (VD: Khóa tài khoản trên Redis session, ẩn Episode/Series trên hệ thống Media Server, v.v.)

        } catch (Exception e) {
            log.error("[PenaltyWorker Error] Lỗi khi xử lý tin nhắn penalty: {}", e.getMessage(), e);
        }
    }
}