package com.talex.server.services.ads;

import com.talex.server.dtos.requests.ads.AdTrackRequestDto;
import java.util.UUID;

public interface AdTrackingService {
    void trackImpressionAsync(AdTrackRequestDto request, UUID accountId, String clientFingerprint);
    void trackClickAsync(AdTrackRequestDto request, UUID accountId, String clientFingerprint);
    void track6sViewAsync(AdTrackRequestDto request, UUID accountId, String clientFingerprint);
}
