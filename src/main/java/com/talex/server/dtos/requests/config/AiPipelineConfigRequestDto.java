package com.talex.server.dtos.requests.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiPipelineConfigRequestDto {

    // Milvus similarity score nằm trong [0, 1] — chặn giá trị vô nghĩa làm hỏng pipeline.
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double fingerprintSimilarityThreshold;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double fingerprintClusterThreshold;

    // Rekognition confidence tính theo phần trăm [0, 100].
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double rekognitionConfidenceThreshold;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double rekognitionViolenceConfidenceThreshold;

    // Tham số kỹ thuật: số đếm/giới hạn phải >= 1 (0 hoặc âm vô nghĩa, VD FPS=0 không lấy frame).
    @NotNull
    @Min(1)
    private Integer fingerprintImageTopK;

    @NotNull
    @Min(1)
    private Integer fingerprintVideoTopK;

    @NotNull
    @Min(1)
    private Integer fingerprintMinMatchSeconds;

    @NotNull
    @Min(1)
    private Integer fingerprintMaxGapSeconds;

    @NotNull
    @Min(1)
    private Integer fingerprintFps;

    @NotNull
    @Min(1)
    private Integer fingerprintMaxFrames;

    // Trần 100 khớp CHÍNH XÁC với FINGERPRINT_MAX_FILE_SIZE_MB tĩnh trong .env Python —
    // đây là cap chặn tải file từ S3 (chống OOM ở kafka_consumer_service.py, cố ý KHÔNG
    // đọc DB động vì nằm trên hot path Kafka). Nếu Admin đặt vượt trần này, giá trị lưu
    // vào DB vẫn không có tác dụng thật (S3 chặn trước khi tới bước validate động) — chặn
    // ngay tại đây để báo lỗi rõ ràng thay vì để Admin tưởng đã tăng giới hạn thành công.
    // Nếu sau này ops đổi FINGERPRINT_MAX_FILE_SIZE_MB trong .env Python, PHẢI đổi con số
    // 100 dưới đây theo cho khớp.
    @NotNull
    @Min(1)
    @Max(value = 100, message = "Không thể vượt quá 100MB — đây là trần cứng khi tải file "
            + "từ S3 (chống OOM), không đọc được cấu hình động. Liên hệ dev nếu cần tăng trần này.")
    private Integer fingerprintMaxFileSizeMb;

    @NotNull
    @Min(1)
    private Integer rekognitionMaxFrames;

    // Cho phép < 1 giây nhưng không được 0/âm.
    @NotNull
    @DecimalMin("0.1")
    private Double moderationFrameInterval;
}
