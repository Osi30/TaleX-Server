package com.talex.server.services.ads;

import com.talex.server.dtos.requests.ads.AdTopupRequestDto;
import com.talex.server.dtos.responses.ads.AdvertiseProfileResponseDto;
import java.util.UUID;

public interface AdWalletService {
    AdvertiseProfileResponseDto getOrCreateProfile(UUID accountId);
    AdvertiseProfileResponseDto setupProfile(UUID accountId, com.talex.server.dtos.requests.ads.AdProfileSetupRequestDto request);
    AdvertiseProfileResponseDto topupWallet(UUID accountId, AdTopupRequestDto request);
    void fundCampaign(UUID profileId, Long amount, UUID campaignId);
    void chargeHeldFunds(UUID profileId, Long amount, UUID campaignId);
    void refundHeldFunds(UUID profileId, Long amount, UUID campaignId, String note);
    java.util.List<com.talex.server.dtos.responses.ads.AdTransactionResponseDto> getWalletTransactions(UUID accountId);
}
