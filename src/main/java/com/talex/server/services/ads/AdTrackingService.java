package com.talex.server.services.ads;

import com.talex.server.dtos.requests.ads.AdTrackRequestDto;

public interface AdTrackingService {
    void trackImpressionAsync(AdTrackRequestDto request);
    void trackClickAsync(AdTrackRequestDto request);
    void track6sViewAsync(AdTrackRequestDto request);
}
