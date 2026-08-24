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
    // Trần trên chặn Admin đẩy giá trị vượt xa mức đã kiểm chứng an toàn cho container
    // AI-Python (mem_limit 4.5g, xem _VIDEO_JOB_SEMAPHORE trong kafka_consumer_service.py) —
    // không có trần, 1 lần đổi config sai có thể làm 4 job video chạy song song tràn RAM.
    @NotNull
    @Min(1)
    @Max(100)
    private Integer fingerprintImageTopK;

    // Nhân với số frame mỗi video (top_k video x max_frames = tổng kết quả Milvus trả về
    // mỗi job) — trần thấp hơn ảnh vì video có tới hàng trăm frame/query.
    @NotNull
    @Min(1)
    @Max(50)
    private Integer fingerprintVideoTopK;

    @NotNull
    @Min(1)
    @Max(3600)
    private Integer fingerprintMinMatchSeconds;

    @NotNull
    @Min(1)
    @Max(3600)
    private Integer fingerprintMaxGapSeconds;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer fingerprintFps;

    // Trần quan trọng nhất chống OOM: extractor.py giữ TOÀN BỘ frame PNG trong RAM cùng lúc
    // (~1.5MB/frame), và _VIDEO_JOB_SEMAPHORE cho phép 4 job video chạy song song. Ở mặc định
    // 300 frame, 4 job đã tốn ~1.8GB RAM (đã đo thật, xem comment kafka_consumer_service.py).
    // Trần 500 giữ tổng RAM 4 job trong khoảng an toàn dưới mem_limit 4.5g của container
    // AI-Python (docker-compose.yml), còn dư cho watermark encode + các service khác cùng chạy.
    @NotNull
    @Min(1)
    @Max(500)
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

    // Frame kiểm duyệt lưu bytes JPEG nén (nhẹ hơn PNG thô của fingerprint), rủi ro chính là
    // tốn chi phí/thời gian gọi AWS Rekognition (ThreadPoolExecutor giới hạn 2 luồng song
    // song, xem video_moderation_service.py) chứ không phải OOM — trần vẫn đặt để chặn giá
    // trị phi thực tế.
    @NotNull
    @Min(1)
    @Max(100)
    private Integer rekognitionMaxFrames;

    // Cho phép < 1 giây nhưng không được 0/âm. Trần trên chỉ để chặn giá trị vô nghĩa
    // (khoảng cách quá lớn không có rủi ro kỹ thuật, chỉ làm giảm độ phủ kiểm duyệt).
    @NotNull
    @DecimalMin("0.1")
    @DecimalMax("60.0")
    private Double moderationFrameInterval;
}
