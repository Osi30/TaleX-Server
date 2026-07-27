package com.talex.server.services.ads;

import com.talex.server.dtos.requests.ads.AdTopupRequestDto;
import com.talex.server.dtos.responses.ads.AdvertiseProfileResponseDto;
import java.util.UUID;

public interface IAdWalletService {
    AdvertiseProfileResponseDto getOrCreateProfile(UUID accountId);
    AdvertiseProfileResponseDto topupWallet(UUID accountId, AdTopupRequestDto request);
    void holdFunds(UUID profileId, Long amount, UUID campaignId);
    void chargeHeldFunds(UUID profileId, Long amount, UUID campaignId);
    void refundHeldFunds(UUID profileId, Long amount, UUID campaignId, String note);
}
