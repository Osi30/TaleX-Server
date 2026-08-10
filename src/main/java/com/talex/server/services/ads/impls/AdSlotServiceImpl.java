package com.talex.server.services.ads.impls;

import com.talex.server.dtos.requests.ads.AdSlotRequestDto;
import com.talex.server.dtos.responses.ads.AdSlotResponseDto;
import com.talex.server.entities.ads.AdSlot;
import com.talex.server.repositories.ads.AdSlotRepository;
import com.talex.server.services.ads.AdSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdSlotServiceImpl implements AdSlotService {

    private final AdSlotRepository slotRepository;

    @Override
    @Transactional
    public AdSlotResponseDto createSlot(AdSlotRequestDto request) {
        if (slotRepository.findByCodeName(request.getCodeName()).isPresent()) {
            throw new RuntimeException("Slot code name already exists");
        }

        AdSlot slot = AdSlot.builder()
                .codeName(request.getCodeName())
                .displayName(request.getDisplayName())
                .type(request.getType())
                .price(request.getPrice())
                .totalViewOfPrice(request.getTotalViewOfPrice())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        return toDto(slotRepository.save(slot));
    }

    @Override
    @Transactional
    public AdSlotResponseDto updateSlot(UUID slotId, AdSlotRequestDto request) {
        AdSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        
        slot.setDisplayName(request.getDisplayName());
        slot.setType(request.getType());
        slot.setPrice(request.getPrice());
        slot.setTotalViewOfPrice(request.getTotalViewOfPrice());
        if (request.getIsActive() != null) {
            slot.setIsActive(request.getIsActive());
        }
        
        return toDto(slotRepository.save(slot));
    }

    @Override
    @Transactional
    public AdSlotResponseDto toggleSlotStatus(UUID slotId, boolean isActive) {
        AdSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        
        slot.setIsActive(isActive);
        return toDto(slotRepository.save(slot));
    }

    @Override
    @Transactional
    public void deleteSlot(UUID slotId) {
        AdSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        slotRepository.delete(slot);
    }

    @Override
    public List<AdSlotResponseDto> getAllSlots() {
        return slotRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdSlotResponseDto> getActiveSlots() {
        return slotRepository.findByIsActiveTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public AdSlotResponseDto getSlotById(UUID slotId) {
        AdSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        return toDto(slot);
    }

    private AdSlotResponseDto toDto(AdSlot slot) {
        return AdSlotResponseDto.builder()
                .slotId(slot.getSlotId())
                .codeName(slot.getCodeName())
                .displayName(slot.getDisplayName())
                .type(slot.getType())
                .price(slot.getPrice())
                .totalViewOfPrice(slot.getTotalViewOfPrice())
                .isActive(slot.getIsActive())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .build();
    }
}
