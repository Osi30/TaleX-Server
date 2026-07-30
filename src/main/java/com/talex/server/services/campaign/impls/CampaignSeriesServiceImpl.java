package com.talex.server.services.campaign.impls;

import com.talex.server.dtos.analytic.CampaignSeriesLogResponseDto;
import com.talex.server.dtos.responses.campaign.CampaignSeriesResponseDto;
import com.talex.server.entities.campaign.CampaignSeries;
import com.talex.server.entities.campaign.CampaignSeriesLog;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.exceptions.codes.CampaignSeriesErrorCode;
import com.talex.server.exceptions.details.CampaignSeriesException;
import com.talex.server.repositories.campaign.CampaignSeriesLogRepository;
import com.talex.server.repositories.campaign.CampaignSeriesRepository;
import com.talex.server.services.campaign.CampaignSeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignSeriesServiceImpl implements CampaignSeriesService {
    private final CampaignSeriesRepository campaignSeriesRepository;
    private final CampaignSeriesLogRepository campaignSeriesLogRepository;

    @Override
    public List<CampaignSeriesResponseDto> getByCampaignId(String campaignId) {
        return campaignSeriesRepository.findByCampaign_CampaignId(campaignId)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public CampaignSeriesResponseDto updateStatus(String campaignSeriesId, CampaignStatus newStatus) {
        CampaignSeries campaignSeries = campaignSeriesRepository.findById(campaignSeriesId)
                .orElseThrow(() -> new CampaignSeriesException(CampaignSeriesErrorCode.NOT_FOUND));

        CampaignStatus currentStatus = campaignSeries.getStatus();

        // Qui tắc: RUNNING -> PAUSED hoặc PAUSED -> RUNNING
        boolean isValidTransition = (currentStatus == CampaignStatus.RUNNING && newStatus == CampaignStatus.PAUSED)
                || (currentStatus == CampaignStatus.PAUSED && newStatus == CampaignStatus.RUNNING);

        if (!isValidTransition) {
            throw new CampaignSeriesException(
                    CampaignSeriesErrorCode.INVALID_STATUS_TRANSITION,
                    "Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus
            );
        }

        campaignSeries.setStatus(newStatus);
        return mapToResponse(campaignSeriesRepository.save(campaignSeries));
    }

    @Override
    @Transactional
    public CampaignSeriesResponseDto cancelCampaignSeries(String campaignSeriesId) {
        CampaignSeries campaignSeries = campaignSeriesRepository.findById(campaignSeriesId)
                .orElseThrow(() -> new CampaignSeriesException(CampaignSeriesErrorCode.NOT_FOUND));

        CampaignStatus currentStatus = campaignSeries.getStatus();

        // Qui tắc: Chỉ kích hoạt khi status là RUNNING hoặc PAUSED
        if (currentStatus != CampaignStatus.RUNNING && currentStatus != CampaignStatus.PAUSED) {
            throw new CampaignSeriesException(
                    CampaignSeriesErrorCode.CANNOT_CANCEL,
                    "Không thể hủy CampaignSeries đang ở trạng thái: " + currentStatus
            );
        }

        campaignSeries.setStatus(CampaignStatus.CANCELLED);
        return mapToResponse(campaignSeriesRepository.save(campaignSeries));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignSeriesLogResponseDto> getLogs(String campaignSeriesId, LocalDateTime startTime, LocalDateTime endTime) {
        List<CampaignSeriesLog> logs = campaignSeriesLogRepository
                .findByCampaignSeries_CampaignSeriesIdAndHourBucketBetweenOrderByHourBucketAsc(
                        campaignSeriesId, startTime, endTime
                );

        return logs.stream()
                .map(log -> CampaignSeriesLogResponseDto.builder()
                        .campaignSeriesLogId(log.getCampaignEpisodeLogId())
                        .campaignSeriesId(log.getCampaignSeries().getCampaignSeriesId())
                        .hourBucket(log.getHourBucket())
                        .analyticData(log.getAnalyticData())
                        .totalImpression(log.getTotalImpression())
                        .build())
                .toList();
    }

    private CampaignSeriesResponseDto mapToResponse(CampaignSeries entity) {
        if (entity == null) {
            return null;
        }

        return CampaignSeriesResponseDto.builder()
                .campaignSeriesId(entity.getCampaignSeriesId())
                .campaignId(entity.getCampaign() != null ? entity.getCampaign().getCampaignId() : null)
                .seriesId(entity.getSeries() != null ? entity.getSeries().getSeriesId() : null)
                .status(entity.getStatus())
                .analyticData(entity.getAnalyticData())
                .totalImpression(entity.getTotalImpression())
                .build();
    }
}
