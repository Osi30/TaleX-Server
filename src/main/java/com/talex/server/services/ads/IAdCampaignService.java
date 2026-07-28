package com.talex.server.services.ads;

import com.talex.server.dtos.requests.ads.AdCampaignCreateRequestDto;
import com.talex.server.dtos.requests.ads.AdCampaignReviewRequestDto;
import com.talex.server.dtos.responses.ads.AdCampaignResponseDto;
import com.talex.server.dtos.responses.ads.AdServeResponseDto;
import com.talex.server.enums.ads.AdCampaignStatus;

import java.util.List;
import java.util.UUID;

public interface IAdCampaignService {
    // User functions
    AdCampaignResponseDto createCampaign(UUID accountId, AdCampaignCreateRequestDto request);
    List<AdCampaignResponseDto> getMyCampaigns(UUID accountId);
    AdCampaignResponseDto updateCampaignLabels(UUID accountId, UUID campaignId, List<String> labels);
    
    List<com.talex.server.dtos.responses.ads.AdMetricResponseDto> getCampaignMetrics(UUID accountId, UUID campaignId);
    
    // Admin functions
    List<AdCampaignResponseDto> getPendingCampaigns();
    List<AdCampaignResponseDto> getAllCampaignsForAdmin();
    AdCampaignResponseDto reviewCampaign(UUID campaignId, AdCampaignReviewRequestDto request);
    AdCampaignResponseDto patchCampaignStatus(UUID campaignId, AdCampaignStatus newStatus);

    List<com.talex.server.entities.ads.AdTransaction> getCampaignTransactions(UUID accountId, UUID campaignId);
    
    // Delivery
    AdServeResponseDto serveAd(String slotCode);
    List<AdServeResponseDto> serveAllAds(String slotCode);
}
