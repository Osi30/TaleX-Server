package com.talex.server.dtos.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outgoing message dispatched to Python AI service via Kafka.
 * Triggers copyright fingerprinting or content moderation pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineJobMessage {
    private String mediaId;
    private String s3Key;
    private String s3Bucket;
    private String mediaType;       // "VIDEO" or "IMAGE"
    private String correlationId;
    private String requestedAt;     // ISO-8601 LocalDateTime string
    // Chỉ dùng cho Content ID (copyright job) — AI loại trừ so khớp trong cùng
    // creator (không tự báo "vi phạm" chính nội dung của mình). Rỗng ở moderation job.
    private String creatorId;
}
