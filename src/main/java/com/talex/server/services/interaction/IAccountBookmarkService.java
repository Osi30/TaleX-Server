package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.response.AccountBookmarkResponse;
import com.talex.server.dtos.interaction.response.EpisodeBookmarkResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface IAccountBookmarkService {
    void bookmarkEpisode(UUID accountId, String episodeId);
    void unbookmarkEpisode(UUID accountId, String episodeId);
    Slice<EpisodeBookmarkResponse> getBookmarksByEpisode(String episodeId, Pageable pageable);
    Slice<AccountBookmarkResponse> getBookmarksByAccount(UUID accountId, Pageable pageable);
}
