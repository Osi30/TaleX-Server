package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.RatingRequest;
import com.talex.server.dtos.interaction.response.AccountRatingResponse;
import com.talex.server.dtos.interaction.response.SeriesRatingResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface IAccountRatingService {
    void upsertRating(RatingRequest requestDto);

    void deleteRating(UUID accountId, String seriesId);

    Slice<AccountRatingResponse> getRatingsByAccount(UUID accountId, Pageable pageable);

    Slice<SeriesRatingResponse> getRatingsBySeries(String seriesId, Pageable pageable);
}
