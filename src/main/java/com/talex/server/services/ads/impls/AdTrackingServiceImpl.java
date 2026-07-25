package com.talex.server.services.ads.impls;

import com.talex.server.dtos.requests.ads.AdTrackRequestDto;
import com.talex.server.entities.ads.AdCampaign;
import com.talex.server.entities.ads.AdMetric;
import com.talex.server.enums.ads.AdCampaignStatus;
import com.talex.server.repositories.ads.AdCampaignRepository;
import com.talex.server.repositories.ads.AdMetricRepository;
import com.talex.server.services.ads.IAdTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdTrackingServiceImpl implements IAdTrackingService {

    private final AdCampaignRepository campaignRepository;
    private final AdMetricRepository metricRepository;

    @Override
    @Async
    @Transactional
    public void trackImpressionAsync(AdTrackRequestDto request) {
        try {
            AdCampaign campaign = campaignRepository.findById(request.getCampaignId()).orElse(null);
            if (campaign == null || campaign.getStatus() != AdCampaignStatus.ACTIVE) return;

            campaign.setCurrentImpressions(campaign.getCurrentImpressions() + 1);
            if (campaign.getCurrentImpressions() >= campaign.getTargetImpressions()) {
                campaign.setStatus(AdCampaignStatus.COMPLETED);
            }
            campaignRepository.save(campaign);

            upsertMetric(campaign, true);
        } catch (Exception e) {
            log.error("Error tracking impression for campaign {}: {}", request.getCampaignId(), e.getMessage());
        }
    }

    @Override
    @Async
    @Transactional
    public void trackClickAsync(AdTrackRequestDto request) {
        try {
            AdCampaign campaign = campaignRepository.findById(request.getCampaignId()).orElse(null);
            if (campaign == null || campaign.getStatus() != AdCampaignStatus.ACTIVE) return;

            campaign.setCurrentClicks(campaign.getCurrentClicks() + 1);
            campaignRepository.save(campaign);

            upsertMetric(campaign, false);
        } catch (Exception e) {
            log.error("Error tracking click for campaign {}: {}", request.getCampaignId(), e.getMessage());
        }
    }

    private void upsertMetric(AdCampaign campaign, boolean isImpression) {
        LocalDate today = LocalDate.now();
        AdMetric metric = metricRepository.findByCampaign_CampaignIdAndReportDate(campaign.getCampaignId(), today)
                .orElseGet(() -> AdMetric.builder()
                        .campaign(campaign)
                        .reportDate(today)
                        .impressions(0L)
                        .clicks(0L)
                        .build());
        
        if (isImpression) {
            metric.setImpressions(metric.getImpressions() + 1);
        } else {
            metric.setClicks(metric.getClicks() + 1);
        }
        
        metricRepository.save(metric);
    }
}
