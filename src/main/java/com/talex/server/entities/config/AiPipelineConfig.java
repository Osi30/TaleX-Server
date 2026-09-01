package com.talex.server.entities.config;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "ai_pipeline_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPipelineConfig extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id", updatable = false, nullable = false)
    private UUID configId;

    // Ngưỡng similarity tạo record vi phạm (Milvus score 0..1).
    @Column(name = "fingerprint_similarity_threshold", nullable = false)
    @Builder.Default
    private Double fingerprintSimilarityThreshold = 0.90;

    // Ngưỡng gán chủ sở hữu cụm — chặt hơn ngưỡng vi phạm (Milvus score 0..1).
    @Column(name = "fingerprint_cluster_threshold", nullable = false)
    @Builder.Default
    private Double fingerprintClusterThreshold = 0.95;

    // Ngưỡng confidence kiểm duyệt chung Rekognition (0..100).
    @Column(name = "rekognition_confidence_threshold", nullable = false)
    @Builder.Default
    private Double rekognitionConfidenceThreshold = 80.0;

    // Ngưỡng riêng, thấp hơn, cho nhóm Violence/Visually Disturbing (0..100).
    @Column(name = "rekognition_violence_confidence_threshold", nullable = false)
    @Builder.Default
    private Double rekognitionViolenceConfidenceThreshold = 60.0;

    // ===== Tham số kỹ thuật/hiệu năng (số nguyên dương; float cho frame interval) =====

    // Số ứng viên top_k Milvus khi so khớp ẢNH (1 vector/query).
    @Column(name = "fingerprint_image_top_k", nullable = false)
    @Builder.Default
    private Integer fingerprintImageTopK = 20;

    // Số ứng viên top_k Milvus khi so khớp VIDEO — set sai từng gây bug đẩy văng nguồn thật.
    @Column(name = "fingerprint_video_top_k", nullable = false)
    @Builder.Default
    private Integer fingerprintVideoTopK = 15;

    // Độ dài đoạn trùng tối thiểu (giây) để tính là vi phạm video.
    @Column(name = "fingerprint_min_match_seconds", nullable = false)
    @Builder.Default
    private Integer fingerprintMinMatchSeconds = 5;

    // Khoảng cách tối đa (giây) giữa 2 điểm vẫn nối chung 1 đoạn.
    @Column(name = "fingerprint_max_gap_seconds", nullable = false)
    @Builder.Default
    private Integer fingerprintMaxGapSeconds = 2;

    // Số frame trích xuất mỗi giây khi tạo fingerprint video.
    @Column(name = "fingerprint_fps", nullable = false)
    @Builder.Default
    private Integer fingerprintFps = 1;

    // Tổng số frame tối đa trích xuất cho fingerprint (chống video dài phình dữ liệu).
    @Column(name = "fingerprint_max_frames", nullable = false)
    @Builder.Default
    private Integer fingerprintMaxFrames = 300;

    // Dung lượng file tối đa (MB) được phép chạy fingerprint.
    @Column(name = "fingerprint_max_file_size_mb", nullable = false)
    @Builder.Default
    private Integer fingerprintMaxFileSizeMb = 100;

    // Số frame tối đa lấy mẫu cho kiểm duyệt Rekognition.
    @Column(name = "rekognition_max_frames", nullable = false)
    @Builder.Default
    private Integer rekognitionMaxFrames = 30;

    // Khoảng cách (giây) giữa các frame lấy mẫu kiểm duyệt.
    @Column(name = "moderation_frame_interval", nullable = false)
    @Builder.Default
    private Double moderationFrameInterval = 2.0;
}
