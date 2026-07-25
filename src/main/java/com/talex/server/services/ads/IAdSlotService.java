package com.talex.server.services.ads;

import com.talex.server.dtos.requests.ads.AdSlotRequestDto;
import com.talex.server.dtos.responses.ads.AdSlotResponseDto;

import java.util.List;
import java.util.UUID;

public interface IAdSlotService {
    AdSlotResponseDto createSlot(AdSlotRequestDto request);
    AdSlotResponseDto updateSlot(UUID slotId, AdSlotRequestDto request);
    AdSlotResponseDto toggleSlotStatus(UUID slotId, boolean isActive);
    void deleteSlot(UUID slotId);
    List<AdSlotResponseDto> getAllSlots();
    List<AdSlotResponseDto> getActiveSlots();
    AdSlotResponseDto getSlotById(UUID slotId);
}
