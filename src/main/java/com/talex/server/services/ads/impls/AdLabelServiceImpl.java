package com.talex.server.services.ads.impls;

import com.talex.server.dtos.requests.ads.AdLabelRequestDto;
import com.talex.server.dtos.responses.ads.AdLabelResponseDto;
import com.talex.server.entities.ads.AdLabel;
import com.talex.server.entities.ads.AdvertiseProfile;
import com.talex.server.exceptions.codes.campaign.CampaignErrorCode;
import com.talex.server.exceptions.details.campaign.CampaignException;
import com.talex.server.repositories.ads.AdLabelRepository;
import com.talex.server.repositories.ads.AdvertiseProfileRepository;
import com.talex.server.services.ads.IAdLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdLabelServiceImpl implements IAdLabelService {

    private final AdLabelRepository adLabelRepository;
    private final AdvertiseProfileRepository advertiseProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdLabelResponseDto> getAllLabels(UUID accountId) {
        return adLabelRepository.findAllByProfile_Account_AccountId(accountId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdLabelResponseDto createLabel(UUID accountId, AdLabelRequestDto requestDto) {
        AdvertiseProfile profile = advertiseProfileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new CampaignException(CampaignErrorCode.NOT_FOUND, "Profile not found"));

        AdLabel label = AdLabel.builder()
                .profile(profile)
                .name(requestDto.getName())
                .color(requestDto.getColor())
                .build();

        return mapToDto(adLabelRepository.save(label));
    }

    @Override
    @Transactional
    public AdLabelResponseDto updateLabel(UUID accountId, UUID labelId, AdLabelRequestDto requestDto) {
        AdLabel label = adLabelRepository.findById(labelId)
                .orElseThrow(() -> new CampaignException(CampaignErrorCode.NOT_FOUND, "Label not found"));

        if (!label.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new CampaignException(CampaignErrorCode.INVALID_REQUEST, "Access denied");
        }

        label.setName(requestDto.getName());
        label.setColor(requestDto.getColor());

        return mapToDto(adLabelRepository.save(label));
    }

    @Override
    @Transactional
    public void deleteLabel(UUID accountId, UUID labelId) {
        AdLabel label = adLabelRepository.findById(labelId)
                .orElseThrow(() -> new CampaignException(CampaignErrorCode.NOT_FOUND, "Label not found"));

        if (!label.getProfile().getAccount().getAccountId().equals(accountId)) {
            throw new CampaignException(CampaignErrorCode.INVALID_REQUEST, "Access denied");
        }

        adLabelRepository.delete(label);
    }

    private AdLabelResponseDto mapToDto(AdLabel label) {
        return AdLabelResponseDto.builder()
                .labelId(label.getLabelId())
                .profileId(label.getProfile().getProfileId())
                .name(label.getName())
                .color(label.getColor())
                .build();
    }
}
