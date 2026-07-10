package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.response.AccountBookmarkResponse;
import com.talex.server.dtos.interaction.response.EpisodeBookmarkResponse;
import com.talex.server.entities.interaction.AccountBookmark;
import com.talex.server.entities.series.Episode;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.AccountBookmarkRepository;
import com.talex.server.services.interaction.IAccountBookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountBookmarkService implements IAccountBookmarkService {
    private final AccountBookmarkRepository accountBookmarkRepository;

    @Override
    @Transactional
    public void bookmarkEpisode(UUID accountId, String episodeId) {
        try {
            int rowsAffected = accountBookmarkRepository.insertBookmarkDirectly(accountId, episodeId);

            if (rowsAffected == 0) {
                throw new InteractionException(
                        InteractionErrorCode.BOOKMARK_ALREADY_EXISTS,
                        "Bạn đã lưu dấu tập phim này rồi."
                );
            }
        } catch (DataIntegrityViolationException e) {
            throw new InteractionException(
                    InteractionErrorCode.BOOKMARK_NOT_FOUND,
                    "Tài khoản hoặc tập phim không tồn tại."
            );
        }
    }

    @Override
    @Transactional
    public void unbookmarkEpisode(UUID accountId, String episodeId) {
        int rowsAffected = accountBookmarkRepository.deleteByAccountIdAndEpisodeId(accountId, episodeId);
        if (rowsAffected == 0) {
            throw new InteractionException(
                    InteractionErrorCode.BOOKMARK_NOT_FOUND,
                    "Bạn chưa bookmark tập phim này hoặc dữ liệu không tồn tại."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<EpisodeBookmarkResponse> getBookmarksByEpisode(String episodeId, Pageable pageable) {
        Slice<AccountBookmark> bookmarks = accountBookmarkRepository.findByEpisodeEpisodeId(episodeId, pageable);

        return bookmarks.map(bookmark -> EpisodeBookmarkResponse.builder()
                .accountId(bookmark.getAccount().getAccountId())
                .username(bookmark.getAccount().getUsername())
                .avatarUrl(bookmark.getAccount().getAvatarUrl())
                .bookmarkedAt(bookmark.getCreatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<AccountBookmarkResponse> getBookmarksByAccount(UUID accountId, Pageable pageable) {
        Slice<AccountBookmark> bookmarks = accountBookmarkRepository.findByAccountAccountId(accountId, pageable);

        return bookmarks.map(bookmark -> {
            Episode ep = bookmark.getEpisode();
            String seriesTitle = (ep.getSeason() != null && ep.getSeason().getSeries() != null)
                    ? ep.getSeason().getSeries().getTitle() : "N/A";
            String seriesCover = (ep.getSeason() != null && ep.getSeason().getSeries() != null)
                    ? ep.getSeason().getSeries().getCoverUrl() : null;

            return AccountBookmarkResponse.builder()
                    .episodeId(ep.getEpisodeId())
                    .episodeTitle(ep.getTitle())
                    .episodeNumber(ep.getEpisodeNumber())
                    .seriesTitle(seriesTitle)
                    .seriesCoverUrl(seriesCover)
                    .bookmarkedAt(bookmark.getCreatedAt())
                    .build();
        });
    }
}