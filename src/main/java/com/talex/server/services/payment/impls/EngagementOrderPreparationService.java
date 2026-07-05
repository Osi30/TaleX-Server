package com.talex.server.services.payment.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.exceptions.codes.EngagementErrorCode;
import com.talex.server.exceptions.codes.PaymentErrorCode;
import com.talex.server.exceptions.details.EngagementServiceException;
import com.talex.server.exceptions.details.PaymentException;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.services.campaign.IEngagementServiceService;
import com.talex.server.services.creator.ICreatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EngagementOrderPreparationService {

    private final IEngagementServiceService engagementServiceService;
    private final ICreatorService creatorService;
    private final EpisodeRepository episodeRepository;
    private final ObjectMapper objectMapper;

    public EngagementService fetchActiveEngagementService(String engagementServiceId) {
        EngagementService engagementService = engagementServiceService.findById(engagementServiceId);
        if (Boolean.FALSE.equals(engagementService.getIsActive())) {
            throw new EngagementServiceException(EngagementErrorCode.INVALID_REQUEST,
                    "Gói dịch vụ tương tác hiện không khả dụng");
        }
        return engagementService;
    }

    public void validateOwnedPublishedEpisodes(UUID accountId, List<String> episodeIds) {
        String creatorId = creatorService.getIdByAccountId(accountId);
        Set<String> uniqueEpisodeIds = new HashSet<>(episodeIds);
        long validCount = episodeRepository.countByEpisodeIdInAndStatusAndIsDeletedFalseAndCreatorId(
                uniqueEpisodeIds, EpisodeStatus.PUBLISHED, creatorId);
        if (validCount != uniqueEpisodeIds.size()) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_OWNED,
                    "Có tập phim không hợp lệ, chưa xuất bản hoặc không thuộc về bạn");
        }
    }

    public String serializeEpisodeIds(List<String> episodeIds) {
        try {
            return objectMapper.writeValueAsString(episodeIds);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize episodeIds for order metadata", exception);
        }
    }
}
