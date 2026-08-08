package com.talex.server.services.series.impls;

import com.talex.server.entities.media.Media;
import com.talex.server.entities.series.Episode;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cascade soft-delete cho cây Series -> Season -> Episode -> Media khi 1 node cha bị xóa,
 * để tránh Episode/Media "zombie" vẫn xem được dù Series/Season cha đã "xóa" (trước đây
 * EpisodeServiceImpl/SeasonServiceImpl/SeriesServiceImpl.delete() chỉ set status trên
 * đúng 1 node, không đụng tới con cháu).
 * <p>
 * Không cascade sang Milvus fingerprint hay xóa file provider (S3/CloudFront) — chỉ soft
 * delete tầng DB. Fingerprint được giữ lại có chủ đích để vẫn bắt được nội dung đạo nhái
 * nếu ai đó re-upload lại đúng nội dung đã xóa (đây là quyết định nghiệp vụ, không phải
 * thiếu sót — xem notifyMediaDeleted() chỉ được gọi ở luồng xóa Media trực tiếp).
 * <p>
 * Tách thành component riêng chỉ phụ thuộc repository (không phụ thuộc EpisodeService/
 * SeasonService/SeriesService) để tránh circular dependency — EpisodeServiceImpl đã phụ
 * thuộc SeasonService, SeasonServiceImpl đã phụ thuộc SeriesService.
 */
@Component
@RequiredArgsConstructor
public class ContentCascadeDeleteHelper {

    private final EpisodeRepository episodeRepository;
    private final MediaRepository mediaRepository;

    public void cascadeDeleteEpisodeMedia(String episodeId, String actorId) {
        List<Media> mediaList = mediaRepository
                .findAllByEpisode_EpisodeIdAndIsDeletedFalseOrderByDisplayOrderAsc(episodeId);
        for (Media media : mediaList) {
            media.setStatus(MediaStatus.DELETED);
            media.softDelete(actorId);
        }
        mediaRepository.saveAll(mediaList);
    }

    public void cascadeDeleteSeasonEpisodes(String seasonId, String actorId) {
        List<Episode> episodes = episodeRepository
                .findAllBySeason_SeasonIdAndIsDeletedFalseOrderByEpisodeNumberAsc(seasonId);
        for (Episode episode : episodes) {
            episode.setStatus(EpisodeStatus.DELETED);
            episode.setReleasedUpdateTime(LocalDateTime.now());
            episode.softDelete();
            cascadeDeleteEpisodeMedia(episode.getEpisodeId(), actorId);
        }
        episodeRepository.saveAll(episodes);
    }
}
