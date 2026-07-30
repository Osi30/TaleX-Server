package com.talex.server.services.media.impls;

import com.talex.server.entities.media.Media;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.services.media.ContentPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fallback cho content pipeline (copyright/kiểm duyệt) bị "mất tích" — nếu gửi Kafka job
 * thất bại KHÔNG ĐỒNG BỘ (mất kết nối Aiven giữa chừng...), ContentPipelineProducer chỉ
 * log lỗi (xem callback whenComplete), không có gì tự động đổi trạng thái media vì lỗi
 * xảy ra sau khi method dispatch đã return bình thường. Không có job này, media sẽ kẹt
 * PENDING/HLS_PROCESSING/HLS_READY vĩnh viễn, không dấu hiệu gì cho Creator biết.
 *
 * Mirrors SqsMediaEventPoller.reconcileStaleHlsJobs() pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentPipelineReconciler {

    // Đo thực tế: xử lý bình thường chỉ vài giây/job (xem debug session) — 10 phút là
    // biên an toàn rộng, tránh false positive do jitter mạng/AWS bình thường.
    private static final int STALE_MINUTES = 10;

    private final MediaRepository mediaRepository;
    private final ContentPipelineService contentPipelineService;

    @Scheduled(fixedDelay = 120_000)
    public void reconcileStalePipelineJobs() {
        List<Media> stale = mediaRepository
                .findAllByStatusInAndApprovalStatusAndUpdatedAtBeforeAndIsDeletedFalseOrderByUpdatedAtAsc(
                        List.of(MediaStatus.PENDING, MediaStatus.HLS_PROCESSING, MediaStatus.HLS_READY),
                        ContentApprovalStatus.PENDING_REVIEW,
                        LocalDateTime.now().minusMinutes(STALE_MINUTES),
                        Pageable.ofSize(20));

        if (stale.isEmpty()) {
            return;
        }

        log.warn("Reconcile: {} media stuck in content pipeline for >{} minutes", stale.size(), STALE_MINUTES);

        for (Media media : stale) {
            try {
                contentPipelineService.markProcessingFailed(
                        media.getMediaId(),
                        "PIPELINE_TIMEOUT",
                        "Quá trình xử lý bản quyền/kiểm duyệt không phản hồi sau " + STALE_MINUTES
                                + " phút — có thể do lỗi gửi/nhận Kafka.");
            } catch (Exception e) {
                log.error("Reconcile failed for mediaId={}", media.getMediaId(), e);
            }
        }
    }
}
