package com.talex.server.services.ads;

import com.talex.server.dtos.requests.ads.AdTrackRequestDto;

public interface IAdTrackingService {
    void trackImpressionAsync(AdTrackRequestDto request);
    void trackClickAsync(AdTrackRequestDto request);
}
