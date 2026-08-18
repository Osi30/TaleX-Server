package com.talex.server.workers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.repositories.campaign.AccountCampaignImpressionRepository;
import com.talex.server.repositories.campaign.CampaignRepository;
import com.talex.server.repositories.campaign.CampaignSeriesLogRepository;
import com.talex.server.repositories.campaign.CampaignSeriesRepository;
import com.talex.server.repositories.trending.AccountImpressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImpressionWorker {

    private final AccountImpressionRepository accountImpressionRepository;
    private final AccountCampaignImpressionRepository accountCampaignImpressionRepository;
    private final CampaignSeriesRepository campaignSeriesRepository;
    private final CampaignSeriesLogRepository campaignSeriesLogRepository;
    private final CampaignRepository campaignRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "home-impression-log-topic",
            groupId = "impression-worker-group"
    )
    @Transactional
    public void processHomeImpressions(String messagePayload) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    messagePayload, new TypeReference<>() {}
            );

            String accountIdStr = (String) payload.get("accountId");
            @SuppressWarnings("unchecked")
            List<String> seriesIds = (List<String>) payload.get("seriesIds");

            if (accountIdStr == null || seriesIds == null || seriesIds.isEmpty()) {
                return;
            }

            UUID accountId = UUID.fromString(accountIdStr);

            // 1. Batch Insert AccountImpression (Toàn hệ thống Series)
            String seriesIdsCsv = String.join(",", seriesIds);
            accountImpressionRepository.insertBatchIfNotExists(accountId, seriesIdsCsv);

            // 2. Chèn AccountCampaignImpression và LẤY DANH SÁCH campaign_series_id MỚI THỰC SỰ ĐƯỢC CHÈN
            List<String> newlyInsertedCsIds = accountCampaignImpressionRepository.insertIfNotExistsAndGetInsertedIds(accountId, seriesIds);

            // Nếu có ít nhất 1 CampaignSeries mới được tạo cho Account này thì mới thực hiện cộng Impression
            if (!newlyInsertedCsIds.isEmpty()) {
                // 2.1. Cập nhật total_impression cho CampaignSeries
                campaignSeriesRepository.incrementImpressionsByCampaignSeriesIds(newlyInsertedCsIds);

                // 2.2. UPSERT cho CampaignSeriesLog
                LocalDateTime currentHourBucket = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
                campaignSeriesLogRepository.upsertBatchLogImpressionsByCampaignSeriesIds(newlyInsertedCsIds, currentHourBucket);

                // 2.3. Cập nhật current_impression cho Campaign
                campaignRepository.incrementCampaignImpressionsByCampaignSeriesIds(newlyInsertedCsIds, LocalDateTime.now());

                // 2.4. Tự động hoàn thành các CampaignSeries đã đạt Target Impression
                int completedCount = campaignSeriesRepository.autoCompleteReachedCampaignSeries();
                if (completedCount > 0) {
                    log.info("[ImpressionWorker] Đã hoàn thành (COMPLETED) {} CampaignSeries do đạt chỉ tiêu Impression!", completedCount);
                }
            }

        } catch (Exception e) {
            log.error("[ImpressionWorker Error] Lỗi khi xử lý tin nhắn impression: {}", e.getMessage(), e);
        }
    }
}