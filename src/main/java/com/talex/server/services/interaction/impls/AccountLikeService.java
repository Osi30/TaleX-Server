package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.response.AccountLikeResponse;
import com.talex.server.dtos.interaction.response.EpisodeLikeResponse;
import com.talex.server.entities.Account;
import com.talex.server.entities.interaction.AccountLike;
import com.talex.server.entities.series.Episode;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.interaction.AccountLikeRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.services.interaction.IAccountLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountLikeService implements IAccountLikeService {
    private final AccountLikeRepository accountLikeRepository;
    private final AccountRepository accountRepository;
    private final EpisodeRepository episodeRepository;

    @Override
    @Transactional
    public void likeEpisode(UUID accountId, String episodeId) {
        try {
            Account accountProxy = accountRepository.getReferenceById(accountId);
            Episode episodeProxy = episodeRepository.getReferenceById(episodeId);

            AccountLike accountLike = AccountLike.builder()
                    .account(accountProxy)
                    .episode(episodeProxy)
                    .build();

            // Ép flush xuống db để bắt biệt lệ ràng buộc ngay lập tức
            accountLikeRepository.saveAndFlush(accountLike);

        } catch (DataIntegrityViolationException e) {
            throw new InteractionException(InteractionErrorCode.LIKE_ALREADY_EXISTS,
                    "Thao tác không hợp lệ. Bạn đã thích tập phim này hoặc thông tin không tồn tại.");
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