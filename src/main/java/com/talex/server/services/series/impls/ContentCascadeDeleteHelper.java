package com.talex.server.services.series.impls;

import com.talex.server.entities.media.Media;
import com.talex.server.entities.series.Episode;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.repositories.media.ContentCensorshipRepository;
import com.talex.server.repositories.media.MediaCopyrightRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
 * CÓ dọn ContentCensorship/MediaCopyright (bản ghi review vi phạm chờ Staff duyệt) — khác
 * với fingerprint Milvus, các bản ghi review này không có giá trị giữ lại khi media cha đã
 * bị xóa (không dùng để chống đạo nhái sau này), để lại chỉ tạo rác dữ liệu mồ côi trỏ tới
 * media không còn tồn tại.
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
    private final MediaCopyrightRepository mediaCopyrightRepository;
    private final ContentCensorshipRepository contentCensorshipRepository;

    public void cascadeDeleteEpisodeMedia(String episodeId, String actorId) {
        List<Media> mediaList = mediaRepository
                .findAllByEpisode_EpisodeIdAndIsDeletedFalseOrderByDisplayOrderAsc(episodeId);
        for (Media media : mediaList) {
            media.setStatus(MediaStatus.DELETED);
            media.softDelete(actorId);
        }
        mediaRepository.saveAll(mediaList);

        List<String> mediaIds = mediaList.stream().map(Media::getMediaId).collect(Collectors.toList());
        if (!mediaIds.isEmpty()) {
            mediaCopyrightRepository.deleteAllByMedia_MediaIdIn(mediaIds);
            contentCensorshipRepository.deleteAllByMedia_MediaIdIn(mediaIds);
        }
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
