package com.talex.server.services.ads.impls;

import com.talex.server.dtos.requests.ads.AdTrackRequestDto;
import com.talex.server.entities.ads.AdCampaign;
import com.talex.server.entities.ads.AdMetric;
import com.talex.server.enums.ads.AdCampaignStatus;
import com.talex.server.enums.ads.AdSlotType;
import com.talex.server.repositories.ads.AdCampaignRepository;
import com.talex.server.repositories.ads.AdMetricRepository;
import com.talex.server.services.ads.AdTrackingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdTrackingServiceImpl implements AdTrackingService {

    private static final Logger log = LoggerFactory.getLogger(AdTrackingServiceImpl.class);

    private final AdCampaignRepository campaignRepository;
    private final AdMetricRepository metricRepository;
    private final com.talex.server.repositories.ads.AdTransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;
    private final AdSystemConfigService systemConfigService;

    private Duration getCooldownDuration(UUID campaignId) {
        try {
            AdCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign != null && campaign.getSlot() != null && systemConfigService != null) {
                AdSlotType slotType = campaign.getSlot().getType();
                if (slotType == AdSlotType.VIDEO) {
                    Integer seconds = systemConfigService.getInVideoConfig().getCooldownSeconds();
                    if (seconds != null && seconds > 0) {
                        return Duration.ofSeconds(seconds);
                    }
                    return Duration.ofSeconds(30);
                } else if (slotType == AdSlotType.POPUP) {
                    Integer minutes = systemConfigService.getPopupConfig().getCooldownMinutes();
                    if (minutes != null && minutes > 0) {
                        return Duration.ofMinutes(minutes);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error reading cooldown config, falling back to default: {}", e.getMessage());
        }
        return Duration.ofMinutes(15);
    }

    private boolean isFraudulentOrDuplicate(String eventType, UUID campaignId, UUID accountId, String clientFingerprint) {
        String fpKey = "ad_freq:" + eventType + ":fp:" + (clientFingerprint != null ? clientFingerprint : "unknown") + ":" + campaignId;
        boolean fpRestricted = Boolean.TRUE.equals(redisTemplate.hasKey(fpKey));

        boolean userRestricted = false;
        String userKey = null;
        if (accountId != null) {
            userKey = "ad_freq:" + eventType + ":user:" + accountId + ":" + campaignId;
            userRestricted = Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
        }

        if (fpRestricted || userRestricted) {
            log.info("Anti-fraud: duplicate {} skipped for campaign {} (FP={}, User={})", eventType, campaignId, clientFingerprint, accountId);
            return true;
        }

        // Save keys into Redis with dynamically configured TTL
        Duration cooldown = getCooldownDuration(campaignId);
        redisTemplate.opsForValue().set(fpKey, "1", cooldown);
        if (userKey != null) {
            redisTemplate.opsForValue().set(userKey, "1", cooldown);
        }

        return false;
    }

    @Override
    @Async
    @Transactional
    public void trackImpressionAsync(AdTrackRequestDto request, UUID accountId, String clientFingerprint) {
        try {
            UUID campaignId = request.getCampaignId();
            boolean isMission = "MISSION".equalsIgnoreCase(request.getSource());

            // If it's a mission reward impression, bypass Redis cooldown check and do not set cooldown
            if (!isMission && isFraudulentOrDuplicate("imp", campaignId, accountId, clientFingerprint)) {
                return;
            }

            AdCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign == null || campaign.getStatus() != AdCampaignStatus.ACTIVE) return;

            long costPerImpression = campaign.getLockedCpm() != null ? campaign.getLockedCpm() / 1000 : 0;

            if (costPerImpression > 0 && campaign.getCampaignBalance() < costPerImpression) {
                campaign.setStatus(AdCampaignStatus.COMPLETED);
                campaignRepository.save(campaign);
                return;
            }

            campaign.setCampaignBalance(campaign.getCampaignBalance() - costPerImpression);
            campaign.setCurrentImpressions(campaign.getCurrentImpressions() + 1);

            if (campaign.getCampaignBalance() <= 0 || (campaign.getEndDate() != null && java.time.LocalDateTime.now().isAfter(campaign.getEndDate()))) {
                campaign.setStatus(AdCampaignStatus.COMPLETED);
            }
            campaignRepository.save(campaign);

            // Log deduction transaction if cost > 0
            if (costPerImpression > 0) {
                com.talex.server.entities.ads.AdTransaction transaction = com.talex.server.entities.ads.AdTransaction.builder()
                        .profile(campaign.getProfile())
                        .campaign(campaign)
                        .amount(costPerImpression)
                        .type(com.talex.server.enums.ads.AdTransactionType.DEDUCT_CAMPAIGN)
                        .note(isMission ? "Trừ phí lượt xem (Nhiệm vụ thưởng)" : "Trừ phí lượt xem")
                        .build();
                transactionRepository.save(transaction);
            }

            upsertMetric(campaign, true);
        } catch (Exception e) {
            log.error("Error tracking impression for campaign {}: {}", request.getCampaignId(), e.getMessage());
        }
    }

    @Override
    @Async
    @Transactional
    public void trackClickAsync(AdTrackRequestDto request, UUID accountId, String clientFingerprint) {
        try {
            UUID campaignId = request.getCampaignId();

            if (isFraudulentOrDuplicate("click", campaignId, accountId, clientFingerprint)) {
                return;
            }

            AdCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign == null || campaign.getStatus() != AdCampaignStatus.ACTIVE) return;

            campaign.setCurrentClicks(campaign.getCurrentClicks() + 1);

            if (campaign.getEndDate() != null && java.time.LocalDateTime.now().isAfter(campaign.getEndDate())) {
                campaign.setStatus(AdCampaignStatus.COMPLETED);
            }
            campaignRepository.save(campaign);

            upsertMetric(campaign, false);
        } catch (Exception e) {
            log.error("Error tracking click for campaign {}: {}", request.getCampaignId(), e.getMessage());
        }
    }

    @Override
    @Async
    @Transactional
    public void track6sViewAsync(AdTrackRequestDto request, UUID accountId, String clientFingerprint) {
        try {
            UUID campaignId = request.getCampaignId();

            if (isFraudulentOrDuplicate("view6s", campaignId, accountId, clientFingerprint)) {
                return;
            }

            AdCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign == null || campaign.getStatus() != AdCampaignStatus.ACTIVE) return;

            campaign.setFocusedViews6s(campaign.getFocusedViews6s() + 1);

            campaignRepository.save(campaign);
        } catch (Exception e) {
            log.error("Error tracking 6s view for campaign {}: {}", request.getCampaignId(), e.getMessage());
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
