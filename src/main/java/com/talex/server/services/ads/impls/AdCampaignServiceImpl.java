package com.talex.server.services.ads.impls;

import com.talex.server.dtos.requests.ads.AdCampaignCreateRequestDto;
import com.talex.server.dtos.requests.ads.AdCampaignReviewRequestDto;
import com.talex.server.dtos.responses.ads.AdCampaignResponseDto;
import com.talex.server.dtos.responses.ads.AdCreativeResponseDto;
import com.talex.server.dtos.responses.ads.AdServeResponseDto;
import com.talex.server.entities.ads.AdCampaign;
import com.talex.server.entities.ads.AdCreative;
import com.talex.server.entities.ads.AdSlot;
import com.talex.server.entities.ads.AdvertiseProfile;
import com.talex.server.enums.ads.AdCampaignStatus;
import com.talex.server.repositories.ads.AdCampaignRepository;
import com.talex.server.repositories.ads.AdCreativeRepository;
import com.talex.server.repositories.ads.AdSlotRepository;
import com.talex.server.repositories.ads.AdvertiseProfileRepository;
import com.talex.server.services.ads.AdCampaignService;
import com.talex.server.services.ads.AdMediaUploadService;
import com.talex.server.services.ads.AdWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdCampaignServiceImpl implements AdCampaignService {

    private final AdCampaignRepository campaignRepository;
    private final AdCreativeRepository creativeRepository;
    private final AdSlotRepository slotRepository;
    private final AdvertiseProfileRepository profileRepository;
    private final com.talex.server.repositories.ads.AdMetricRepository metricRepository;
    private final com.talex.server.repositories.ads.AdTransactionRepository transactionRepository;
    private final AdWalletService walletService;
    private final AdMediaUploadService adMediaUploadService;
    private final Random random = new Random();

    @Override
    @Transactional
    public AdCampaignResponseDto createCampaign(UUID accountId, AdCampaignCreateRequestDto request) {
        AdvertiseProfile profile = profileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Ad profile not found. Please top up first."));
        
        AdSlot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Ad slot not found"));

        if (!slot.getIsActive()) {
            throw new RuntimeException("Ad slot is not active");
        }

        if (profile.getWalletBalance() < request.getCampaignBudget()) {
            throw new RuntimeException("Số dư Ví tổng không đủ để cấp ngân sách cho chiến dịch này.");
        }

        long cpm = 0;
        if (slot.getTotalViewOfPrice() != null && slot.getTotalViewOfPrice() > 0) {
            cpm = (slot.getPrice() * 1000) / slot.getTotalViewOfPrice();
        }
        
        AdCampaign campaign = AdCampaign.builder()
                .profile(profile)
                .slot(slot)
                .name(request.getName())
                .targetImpressions(request.getTargetImpressions() != null ? request.getTargetImpressions() : 0L)
                .targetClicks(request.getTargetClicks() != null ? request.getTargetClicks() : 0L)
                .totalBudget(request.getCampaignBudget())
                .campaignBalance(0L) // Will be updated by fundCampaign
                .lockedCpm(cpm)
                .status(AdCampaignStatus.PENDING_REVIEW)
                .labels(request.getLabels() != null ? request.getLabels() : new java.util.ArrayList<>())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        
        campaign = campaignRepository.save(campaign);

        AdCreative creative = AdCreative.builder()
                .campaign(campaign)
                .mediaType(request.getMediaType())
                .mediaUrl(request.getMediaUrl())
                .targetUrl(request.getTargetUrl())
                .build();
        
        creativeRepository.save(creative);

        // Nạp tiền từ Ví Tổng vào Ví Chiến Dịch
        walletService.fundCampaign(profile.getProfileId(), request.getCampaignBudget(), campaign.getCampaignId());

        return toDto(campaign, List.of(creative));
    }

    @Override
    public List<AdCampaignResponseDto> getMyCampaigns(UUID accountId) {
        AdvertiseProfile profile = profileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Ad profile not found"));
        return campaignRepository.findByProfile_ProfileIdOrderByCreatedAtDesc(profile.getProfileId()).stream()
                .map(c -> toDto(c, c.getCreatives()))
                .collect(Collectors.toList());
    }

    @Override
    public AdCampaignResponseDto updateCampaignLabels(UUID accountId, UUID campaignId, List<String> labels) {
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }

        campaign.setLabels(labels != null ? labels : new java.util.ArrayList<>());
        campaign = campaignRepository.save(campaign);
        return toDto(campaign, campaign.getCreatives());
    }

    @Override
    public List<com.talex.server.dtos.responses.ads.AdMetricResponseDto> getCampaignMetrics(UUID accountId, UUID campaignId) {
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }

        return metricRepository.findByCampaign_CampaignIdOrderByReportDateAsc(campaignId).stream()
                .map(m -> com.talex.server.dtos.responses.ads.AdMetricResponseDto.builder()
                        .reportDate(m.getReportDate())
                        .impressions(m.getImpressions())
                        .clicks(m.getClicks())
                        .focusedViews6s(m.getFocusedViews6s())
                        .spend(m.getImpressions() * (campaign.getLockedCpm() != null ? campaign.getLockedCpm() : 0) / 1000)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<com.talex.server.entities.ads.AdTransaction> getCampaignTransactions(UUID accountId, UUID campaignId) {
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }

        return transactionRepository.findByCampaign_CampaignIdOrderByCreatedAtDesc(campaignId);
    }

    @Override
    public List<AdCampaignResponseDto> getPendingCampaigns() {
        return campaignRepository.findByStatusOrderByCreatedAtDesc(AdCampaignStatus.PENDING_REVIEW).stream()
                .map(c -> toDto(c, c.getCreatives()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AdCampaignResponseDto> getAllCampaignsForAdmin() {
        return campaignRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(c -> toDto(c, c.getCreatives()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdCampaignResponseDto reviewCampaign(UUID campaignId, AdCampaignReviewRequestDto request) {
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (campaign.getStatus() != AdCampaignStatus.PENDING_REVIEW) {
            throw new RuntimeException("Campaign is not pending review");
        }

        if (request.getStatus() == AdCampaignStatus.ACTIVE) {
            campaign.setStatus(AdCampaignStatus.ACTIVE);
            if (campaign.getStartDate() == null) {
                campaign.setStartDate(java.time.LocalDateTime.now());
            }
            // walletService.chargeHeldFunds(campaign.getProfile().getProfileId(), campaign.getTotalBudget(), campaign.getCampaignId());
        } else if (request.getStatus() == AdCampaignStatus.REJECTED) {
            campaign.setStatus(AdCampaignStatus.REJECTED);
            campaign.setAdminNote(request.getAdminNote());
            walletService.refundHeldFunds(campaign.getProfile().getProfileId(), campaign.getTotalBudget(), campaign.getCampaignId(), "Refund due to rejection: " + request.getAdminNote());
        } else {
            throw new RuntimeException("Invalid status for review");
        }

        return toDto(campaignRepository.save(campaign), campaign.getCreatives());
    }

    @Override
    public AdCampaignResponseDto patchCampaignStatus(UUID campaignId, AdCampaignStatus newStatus) {
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (campaign.getStatus() == AdCampaignStatus.PENDING_PAYMENT || campaign.getStatus() == AdCampaignStatus.PENDING_REVIEW || campaign.getStatus() == AdCampaignStatus.REJECTED) {
            throw new RuntimeException("Cannot change status of a campaign in this state");
        }
        
        if (newStatus == AdCampaignStatus.ACTIVE || newStatus == AdCampaignStatus.PAUSED) {
            campaign.setStatus(newStatus);
        } else {
            throw new RuntimeException("Only ACTIVE or PAUSED status updates are allowed via this endpoint");
        }

        return toDto(campaignRepository.save(campaign), campaign.getCreatives());
    }

    @Override
    public AdServeResponseDto serveAd(String slotCode) {
        List<AdCampaign> activeCampaigns = campaignRepository.findActiveCampaignsForSlot(slotCode);
        
        if (activeCampaigns.isEmpty()) {
            return null; // Return nothing to trigger AdSense fallback on FE
        }

        // Randomly pick one weighted by something, or just uniformly for now
        AdCampaign pickedCampaign = activeCampaigns.get(random.nextInt(activeCampaigns.size()));
        
        if (pickedCampaign.getCreatives().isEmpty()) {
            return null;
        }

        AdCreative creative = pickedCampaign.getCreatives().get(0);
        String signedUrl = adMediaUploadService.generatePresignedGetUrl(creative.getMediaUrl());

        return AdServeResponseDto.builder()
                .campaignId(pickedCampaign.getCampaignId())
                .mediaUrl(signedUrl)
                .targetUrl(creative.getTargetUrl())
                .mediaType(creative.getMediaType().name())
                .build();
    }

    @Override
    public List<AdServeResponseDto> serveAllAds(String slotCode) {
        List<AdCampaign> activeCampaigns = campaignRepository.findActiveCampaignsForSlot(slotCode);
        
        return activeCampaigns.stream()
                .filter(c -> !c.getCreatives().isEmpty())
                .map(c -> {
                    AdCreative creative = c.getCreatives().get(0);
                    String signedUrl = adMediaUploadService.generatePresignedGetUrl(creative.getMediaUrl());
                    return AdServeResponseDto.builder()
                            .campaignId(c.getCampaignId())
                            .mediaUrl(signedUrl)
                            .targetUrl(creative.getTargetUrl())
                            .mediaType(creative.getMediaType().name())
                            .build();
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private AdCampaignResponseDto toDto(AdCampaign campaign, List<AdCreative> creatives) {
        return AdCampaignResponseDto.builder()
                .campaignId(campaign.getCampaignId())
                .profileId(campaign.getProfile().getProfileId())
                .slotId(campaign.getSlot().getSlotId())
                .slotCodeName(campaign.getSlot().getCodeName())
                .slotType(campaign.getSlot().getType().name())
                .name(campaign.getName())
                .status(campaign.getStatus())
                .campaignBalance(campaign.getCampaignBalance())
                .targetImpressions(campaign.getTargetImpressions())
                .targetClicks(campaign.getTargetClicks())
                .currentImpressions(campaign.getCurrentImpressions())
                .currentClicks(campaign.getCurrentClicks())
                .focusedViews6s(campaign.getFocusedViews6s())
                .totalBudget(campaign.getTotalBudget())
                .lockedCpm(campaign.getLockedCpm())
                .adminNote(campaign.getAdminNote())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .createdAt(campaign.getCreatedAt())
                .labels(campaign.getLabels() != null ? new java.util.ArrayList<>(campaign.getLabels()) : new java.util.ArrayList<>())
                .creatives(creatives != null ? creatives.stream().map(this::toCreativeDto).collect(Collectors.toList()) : List.of())
                .build();
    }

    private AdCreativeResponseDto toCreativeDto(AdCreative creative) {
        String signedUrl = adMediaUploadService.generatePresignedGetUrl(creative.getMediaUrl());
        return AdCreativeResponseDto.builder()
                .creativeId(creative.getCreativeId())
                .mediaType(creative.getMediaType())
                .mediaUrl(signedUrl)
                .targetUrl(creative.getTargetUrl())
                .build();
    }

    @Override
    public void toggleCampaign(UUID accountId, UUID campaignId) {
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (campaign.getStatus() == AdCampaignStatus.ACTIVE) {
            campaign.setStatus(AdCampaignStatus.PAUSED);
        } else if (campaign.getStatus() == AdCampaignStatus.PAUSED) {
            campaign.setStatus(AdCampaignStatus.ACTIVE);
        } else {
            throw new RuntimeException("Cannot toggle campaign in status " + campaign.getStatus());
        }

        campaignRepository.save(campaign);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void cancelCampaign(UUID accountId, UUID campaignId) {
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (campaign.getStatus() == AdCampaignStatus.COMPLETED ||
            campaign.getStatus() == AdCampaignStatus.CANCELLED ||
            campaign.getStatus() == AdCampaignStatus.REJECTED) {
            throw new RuntimeException("Cannot cancel campaign in status " + campaign.getStatus());
        }

        Long balanceToRefund = campaign.getCampaignBalance();

        if (balanceToRefund > 0) {
            AdvertiseProfile profile = campaign.getProfile();
            profile.setWalletBalance(profile.getWalletBalance() + balanceToRefund);
            profileRepository.save(profile);

            campaign.setCampaignBalance(0L);
            campaign.setTotalBudget(campaign.getTotalBudget() - balanceToRefund);

            com.talex.server.entities.ads.AdTransaction transaction = com.talex.server.entities.ads.AdTransaction.builder()
                    .profile(profile)
                    .campaign(campaign)
                    .amount(balanceToRefund)
                    .type(com.talex.server.enums.ads.AdTransactionType.REFUND)
                    .note("Hoàn tiền khi hủy chiến dịch")
                    .build();
            transactionRepository.save(transaction);
        }

        campaign.setStatus(AdCampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
    }

    @Override
    public void updateCampaignSchedule(UUID accountId, UUID campaignId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (campaign.getStatus() != AdCampaignStatus.PAUSED && campaign.getStatus() != AdCampaignStatus.PENDING_REVIEW) {
            throw new RuntimeException("Chỉ có thể thay đổi lịch khi chiến dịch đang Tạm dừng (PAUSED) hoặc Chờ duyệt (PENDING_REVIEW)");
        }

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new RuntimeException("End date must be after start date");
        }

        campaign.setStartDate(startDate);
        campaign.setEndDate(endDate);
        campaignRepository.save(campaign);
    }

    @Override
    @Transactional
    public AdCampaignResponseDto cloneCampaign(UUID accountId, UUID campaignId, Long newBudget, Long newTargetImpressions, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        AdCampaign oldCampaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!oldCampaign.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (oldCampaign.getStatus() != AdCampaignStatus.CANCELLED && oldCampaign.getStatus() != AdCampaignStatus.COMPLETED) {
            throw new RuntimeException("Chỉ có thể clone các chiến dịch đã Hủy hoặc Hoàn thành");
        }

        AdvertiseProfile profile = oldCampaign.getProfile();

        if (profile.getWalletBalance() < newBudget) {
            throw new RuntimeException("Số dư Ví tổng không đủ để cấp ngân sách cho chiến dịch clone này.");
        }

        AdCampaign newCampaign = AdCampaign.builder()
                .profile(profile)
                .slot(oldCampaign.getSlot())
                .name(oldCampaign.getName() + " - Copy")
                .targetImpressions(newTargetImpressions)
                .targetClicks(0L) // reset
                .totalBudget(newBudget)
                .campaignBalance(0L) // Sẽ được nạp sau
                .lockedCpm((long) oldCampaign.getSlot().getPrice()) // Use current slot price
                .status(AdCampaignStatus.PENDING_REVIEW)
                .labels(oldCampaign.getLabels() != null ? new java.util.ArrayList<>(oldCampaign.getLabels()) : new java.util.ArrayList<>())
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        newCampaign = campaignRepository.save(newCampaign);

        List<AdCreative> newCreatives = new java.util.ArrayList<>();
        if (oldCampaign.getCreatives() != null && !oldCampaign.getCreatives().isEmpty()) {
            AdCreative oldCreative = oldCampaign.getCreatives().get(0);
            AdCreative newCreative = AdCreative.builder()
                    .campaign(newCampaign)
                    .mediaType(oldCreative.getMediaType())
                    .mediaUrl(oldCreative.getMediaUrl()) // Reuse media
                    .targetUrl(oldCreative.getTargetUrl())
                    .build();
            creativeRepository.save(newCreative);
            newCreatives.add(newCreative);
        }

        // Nạp tiền từ Ví Tổng vào Ví Chiến Dịch
        walletService.fundCampaign(profile.getProfileId(), newBudget, newCampaign.getCampaignId());

        return toDto(newCampaign, newCreatives);
    }

    @Override
    public AdCampaignResponseDto renameCampaign(UUID accountId, UUID campaignId, String newName) {
        AdCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!campaign.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new RuntimeException("Unauthorized");
        }

        campaign.setName(newName);
        campaign = campaignRepository.save(campaign);
        return toDto(campaign, campaign.getCreatives());
    }

    @Override
    @Transactional
    public void bulkCancelCampaigns(UUID accountId, List<UUID> campaignIds) {
        for (UUID campaignId : campaignIds) {
            try {
                this.cancelCampaign(accountId, campaignId);
            } catch (Exception e) {
                // Ignore specific failures in bulk cancel (e.g. already cancelled)
            }
        }
    }
}
