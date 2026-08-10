package com.talex.server.services.ads;

import com.talex.server.dtos.requests.ads.AdLabelRequestDto;
import com.talex.server.dtos.responses.ads.AdLabelResponseDto;

import java.util.List;
import java.util.UUID;

public interface AdLabelService {
    List<AdLabelResponseDto> getAllLabels(UUID accountId);
    AdLabelResponseDto createLabel(UUID accountId, AdLabelRequestDto requestDto);
    AdLabelResponseDto updateLabel(UUID accountId, UUID labelId, AdLabelRequestDto requestDto);
    void deleteLabel(UUID accountId, UUID labelId);
}
