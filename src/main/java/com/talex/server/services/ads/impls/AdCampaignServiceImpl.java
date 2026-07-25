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
import com.talex.server.services.ads.IAdCampaignService;
import com.talex.server.services.ads.IAdWalletService;
import com.talex.server.services.media.MediaProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdCampaignServiceImpl implements IAdCampaignService {

    private final AdCampaignRepository campaignRepository;
    private final AdCreativeRepository creativeRepository;
    private final AdSlotRepository slotRepository;
    private final AdvertiseProfileRepository profileRepository;
    private final IAdWalletService walletService;
    private final com.talex.server.services.ads.IAdMediaUploadService adMediaUploadService;
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

        // Calculate total budget
        long totalBudget = (long) Math.ceil((double) request.getTargetImpressions() / slot.getTotalViewOfPrice()) * slot.getPrice();

        if (profile.getWalletBalance() < totalBudget) {
            throw new RuntimeException("Insufficient wallet balance. Need: " + totalBudget);
        }

        AdCampaign campaign = AdCampaign.builder()
                .profile(profile)
                .slot(slot)
                .name(request.getName())
                .targetImpressions(request.getTargetImpressions())
                .totalBudget(totalBudget)
                .status(AdCampaignStatus.PENDING_REVIEW)
                .build();
        
        campaign = campaignRepository.save(campaign);

        AdCreative creative = AdCreative.builder()
                .campaign(campaign)
                .mediaType(request.getMediaType())
                .mediaUrl(request.getMediaUrl())
                .targetUrl(request.getTargetUrl())
                .build();
        
        creativeRepository.save(creative);

        // Hold funds
        walletService.holdFunds(profile.getProfileId(), totalBudget, campaign.getCampaignId());

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
            walletService.chargeHeldFunds(campaign.getProfile().getProfileId(), campaign.getTotalBudget(), campaign.getCampaignId());
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
                .name(campaign.getName())
                .status(campaign.getStatus())
                .targetImpressions(campaign.getTargetImpressions())
                .currentImpressions(campaign.getCurrentImpressions())
                .currentClicks(campaign.getCurrentClicks())
                .totalBudget(campaign.getTotalBudget())
                .adminNote(campaign.getAdminNote())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .createdAt(campaign.getCreatedAt())
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
}
