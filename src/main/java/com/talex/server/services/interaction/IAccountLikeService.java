package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.response.AccountLikeResponse;
import com.talex.server.dtos.interaction.response.EpisodeLikeResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface IAccountLikeService {
    void likeEpisode(UUID accountId, String episodeId);
    void unlikeEpisode(UUID accountId, String episodeId);
    Slice<EpisodeLikeResponse> getLikesByEpisode(String episodeId, Pageable pageable);
    Slice<AccountLikeResponse> getLikesByAccount(UUID accountId, Pageable pageable);
}
