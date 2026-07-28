package com.talex.server.services.ads;

import com.talex.server.dtos.requests.ads.AdCampaignCreateRequestDto;
import com.talex.server.dtos.requests.ads.AdCampaignReviewRequestDto;
import com.talex.server.dtos.responses.ads.AdCampaignResponseDto;
import com.talex.server.dtos.responses.ads.AdServeResponseDto;

import java.util.List;
import java.util.UUID;

public interface IAdCampaignService {
    // User functions
    AdCampaignResponseDto createCampaign(UUID accountId, AdCampaignCreateRequestDto request);
    List<AdCampaignResponseDto> getMyCampaigns(UUID accountId);
    
    List<com.talex.server.dtos.responses.ads.AdMetricResponseDto> getCampaignMetrics(UUID accountId, UUID campaignId);
    
    // Admin functions
    List<AdCampaignResponseDto> getPendingCampaigns();
    List<AdCampaignResponseDto> getAllCampaignsForAdmin();
    AdCampaignResponseDto reviewCampaign(UUID campaignId, AdCampaignReviewRequestDto request);
    AdCampaignResponseDto patchCampaignStatus(UUID campaignId, com.talex.server.enums.ads.AdCampaignStatus status);
    
    // Core Engine (Serving)
    AdServeResponseDto serveAd(String slotCode);
    List<AdServeResponseDto> serveAllAds(String slotCode);
}
