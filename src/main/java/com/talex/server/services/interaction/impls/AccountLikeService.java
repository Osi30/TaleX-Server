package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.response.AccountLikeResponse;
import com.talex.server.dtos.interaction.response.EpisodeLikeResponse;
import com.talex.server.entities.interaction.AccountLike;
import com.talex.server.entities.series.Episode;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.AccountLikeRepository;
import com.talex.server.services.interaction.IAccountLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountLikeService implements IAccountLikeService {
    private final AccountLikeRepository accountLikeRepository;

    @Override
    @Transactional
    public void likeEpisode(UUID accountId, String episodeId) {
        try {
            // Bước tối ưu: Gọi duy nhất 1 câu lệnh INSERT thẳng xuống DB
            int rowsAffected = accountLikeRepository.insertLikeDirectly(accountId, episodeId, LocalDateTime.now());

            // Nếu không có hàng nào được thêm (rowsAffected == 0), tức là đã bị trùng lặp
            if (rowsAffected == 0) {
                throw new InteractionException(
                        InteractionErrorCode.LIKE_ALREADY_EXISTS,
                        "Bạn đã thích tập phim này rồi."
                );
            }

        } catch (DataIntegrityViolationException e) {
            throw new InteractionException(
                    InteractionErrorCode.LIKE_NOT_FOUND,
                    "Tài khoản hoặc tập phim không tồn tại."
            );
        }
    }

    @Override
    @Transactional
    public void unlikeEpisode(UUID accountId, String episodeId) {
        int rowsDeleted = accountLikeRepository.deleteByAccountIdAndEpisodeId(accountId, episodeId);

        if (rowsDeleted == 0) {
            throw new InteractionException(InteractionErrorCode.LIKE_NOT_FOUND, "Bạn chưa thích tập phim này hoặc bản ghi không tồn tại.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<EpisodeLikeResponse> getLikesByEpisode(String episodeId, Pageable pageable) {
        Slice<AccountLike> likes = accountLikeRepository.findByEpisodeEpisodeId(episodeId, pageable);

        return likes.map(like -> EpisodeLikeResponse.builder()
                .accountId(like.getAccount().getAccountId())
                .username(like.getAccount().getUsername())
                .avatarUrl(like.getAccount().getAvatarUrl())
                .likedAt(like.getCreatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<AccountLikeResponse> getLikesByAccount(UUID accountId, Pageable pageable) {
        Slice<AccountLike> likes = accountLikeRepository.findByAccountAccountId(accountId, pageable);

        return likes.map(like -> {
            Episode ep = like.getEpisode();
            String seriesTitle = (ep.getSeason() != null && ep.getSeason().getSeries() != null)
                    ? ep.getSeason().getSeries().getTitle() : "N/A";
            String seriesCover = (ep.getSeason() != null && ep.getSeason().getSeries() != null)
                    ? ep.getSeason().getSeries().getCoverUrl() : null;

            return AccountLikeResponse.builder()
                    .episodeId(ep.getEpisodeId())
                    .episodeTitle(ep.getTitle())
                    .episodeNumber(ep.getEpisodeNumber())
                    .seriesTitle(seriesTitle)
                    .seriesCoverUrl(seriesCover)
                    .likedAt(like.getCreatedAt())
                    .build();
        });
    }
}