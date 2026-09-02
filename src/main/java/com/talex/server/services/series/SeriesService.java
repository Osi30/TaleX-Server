package com.talex.server.services.series;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.analytic.SeriesLogResponseDto;
import com.talex.server.dtos.recommend.response.SeriesCardResponseDto;
import com.talex.server.dtos.requests.series.SeriesRequestDto;
import com.talex.server.dtos.requests.series.SeriesSearchCriteria;
import com.talex.server.dtos.responses.series.SeriesResponseDto;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.series.SeriesStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SeriesService {
    SeriesResponseDto create(SeriesRequestDto request, UUID accountId);

    SeriesResponseDto getById(String id, String accountId);

    SeriesResponseDto getPublicById(String id);

    BasePageResponse<SeriesResponseDto> list(Integer page, Integer pageSize);

    BasePageResponse<SeriesResponseDto> listPublic(Integer page, Integer pageSize);

    BasePageResponse<SeriesResponseDto> listByCreator(UUID accountId, List<SeriesStatus> statuses, Integer page, Integer pageSize);

    BasePageResponse<SeriesResponseDto> listByCreatorAndCampaign(UUID accountId, List<SeriesStatus> statuses, Integer page, Integer pageSize);

    Slice<SeriesCardResponseDto> searchPublicSeries(SeriesSearchCriteria criteria, Pageable pageable);

    List<SeriesCardResponseDto> getSeriesCardsByIds(List<String> seriesIds);

    List<SeriesCardResponseDto> getPromotedSeriesCardsByIds(List<String> seriesIds);

    SeriesResponseDto update(String id, SeriesRequestDto request, String accountId);

    SeriesResponseDto hide(String id, String actorId);

    SeriesResponseDto unhide(String id, String actorId);

    SeriesResponseDto forceHide(String id, String actorId);

    SeriesResponseDto forceUnhide(String id, String actorId);

    void delete(String id, String actorId);

    Series findActiveEntity(String id);

    Series findPublicEntity(String id);

    List<SeriesLogResponseDto> getSeriesLogs(String id, LocalDateTime start, LocalDateTime end, String accountId);
}
