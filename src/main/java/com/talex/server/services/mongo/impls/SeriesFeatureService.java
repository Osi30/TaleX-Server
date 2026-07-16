package com.talex.server.services.mongo.impls;

import com.talex.server.entities.SyncMetadata;
import com.talex.server.entities.mongo.SeriesMetadata;
import com.talex.server.entities.mongo.seriesfeatures.SeriesEngagementStats;
import com.talex.server.entities.mongo.seriesfeatures.SeriesInteractionStats;
import com.talex.server.entities.series.Category;
import com.talex.server.entities.series.Series;
import com.talex.server.entities.series.Tag;
import com.talex.server.enums.SyncType;
import com.talex.server.exceptions.codes.MongoDocumentErrorCode;
import com.talex.server.exceptions.details.MongoDocumentException;
import com.talex.server.records.SeriesLogData;
import com.talex.server.repositories.SyncMetadataRepository;
import com.talex.server.repositories.mongo.SeriesMetadataRepository;
import com.talex.server.repositories.series.SeriesLogRepository;
import com.talex.server.services.mongo.ISeriesFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeriesFeatureService implements ISeriesFeatureService {
    private final SeriesMetadataRepository seriesMetadataRepository;
    private final SeriesLogRepository seriesLogRepository;
    private final SyncMetadataRepository syncMetadataRepository;
    private final MongoTemplate mongoTemplate;

    @Async("mongoDbExecutor")
    @Override
    public void saveSeriesMetadata(Series series, Map<String, Category> categories, Map<String, Tag> tags) {
        try {
            List<String> categoryNames = categories != null ? categories.values().stream().map(Category::getCategoryName).toList() : List.of();
            List<String> tagNames = tags != null ? tags.values().stream().map(Tag::getTagName).toList() : List.of();

            SeriesMetadata metadata = SeriesMetadata.builder()
                    .id(series.getSeriesId())
                    .contentType(series.getContentType() != null ? series.getContentType().name() : null)
                    .title(series.getTitle())
                    .description(series.getDescription())
                    .category(categoryNames)
                    .tags(tagNames)
                    .ageRating(series.getAgeRating())
                    .language(series.getLanguage())
                    .creatorTier(series.getCreator() != null && series.getCreator().getCreatorTier() != null ? series.getCreator().getCreatorTier().getTierName() : null)
                    .rating(series.getAverageRating())
                    .bannerUrl(series.getBannerUrl())
                    .coverUrl(series.getCoverUrl())
                    .releasedUpdatedAt(LocalDateTime.now())
                    .build();

            seriesMetadataRepository.save(metadata);
        } catch (Exception e) {
            // Log but don't block the main transaction if Mongo fails
            log.error("Failed to save SeriesMetadata to MongoDB", e);
        }
    }

    @Override
    public void syncAllSeriesFeatures() {
        LocalDateTime currentHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        SyncMetadata syncMetadata = syncMetadataRepository.findById(SyncType.SERIES_DYNAMIC_SYNC)
                .orElse(SyncMetadata.builder()
                        .syncType(SyncType.SERIES_DYNAMIC_SYNC)
                        .lastSyncTime(LocalDateTime.of(1970, 1, 1, 0, 0).atZone(ZoneId.systemDefault()).toInstant())
                        .build());

        LocalDateTime lastSyncTime = LocalDateTime.ofInstant(syncMetadata.getLastSyncTime(), ZoneId.systemDefault());

        CompletableFuture<Void> cumulativeTask = syncCumulativeStats(lastSyncTime, currentHour);
        CompletableFuture<Void> task24h = syncLast24hStats(currentHour);
        CompletableFuture<Void> task7d = syncLast7dStats(currentHour);

        CompletableFuture.allOf(cumulativeTask, task24h, task7d).join();

        syncMetadata.setLastSyncTime(currentHour.minusHours(1).atZone(ZoneId.systemDefault()).toInstant());
        syncMetadataRepository.save(syncMetadata);
    }

    @Async("mongoDbExecutor")
    public CompletableFuture<Void> syncCumulativeStats(LocalDateTime start, LocalDateTime end) {
        try {
            List<SeriesLogData> results = seriesLogRepository.aggregateByHourBucketBetweenExclusive(start, end);
            if (results.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            for (SeriesLogData row : results) {
                String seriesId = row.seriesId();

                // Lấy ra từ Repo, nếu null (chưa từng tồn tại trong MongoDB) thì tạo mới instance trống ngay trong Java heap
                SeriesMetadata existing = seriesMetadataRepository.findById(seriesId)
                        .orElseGet(() -> SeriesMetadata.builder()
                                .id(seriesId)
                                .interactionStats(new SeriesInteractionStats())
                                .engagementStats(new SeriesEngagementStats())
                                .build());

                SeriesInteractionStats currentInteractions = existing.getInteractionStats();
                if (currentInteractions == null) currentInteractions = new SeriesInteractionStats();

                SeriesEngagementStats currentEngagement = existing.getEngagementStats();
                if (currentEngagement == null) currentEngagement = new SeriesEngagementStats();

                // Thực hiện cộng dồn dữ liệu an toàn
                long newClicks = currentInteractions.getTotalClicks() + safeLong(row.totalClicks());
                long newLikes = currentInteractions.getTotalLikes() + safeLong(row.totalLikes());
                long newBookmarks = currentInteractions.getTotalBookmarks() + safeLong(row.totalBookmarks());
                long newShares = currentInteractions.getTotalShares() + safeLong(row.totalShares());
                long newComments = currentInteractions.getTotalComments() + safeLong(row.totalComments());
                double newWatchTime = currentEngagement.getTotalWatchTime() + safeDouble(row.watchTime());


                double likeRatio = newClicks > 0 ? (double) newLikes / newClicks : 0.0;
                double bookmarkRatio = newClicks > 0 ? (double) newBookmarks / newClicks : 0.0;
                double shareRatio = newClicks > 0 ? (double) newShares / newClicks : 0.0;
                double commentRatio = newClicks > 0 ? (double) newComments / newClicks : 0.0;

                // Gán trực tiếp giá trị vào Object
                currentInteractions.setTotalClicks(newClicks);
                currentInteractions.setTotalLikes(newLikes);
                currentInteractions.setTotalBookmarks(newBookmarks);
                currentInteractions.setTotalShares(newShares);
                currentInteractions.setTotalComments(newComments);
                currentInteractions.setLikeToClickRatio(likeRatio);
                currentInteractions.setBookmarkToClickRatio(bookmarkRatio);
                currentInteractions.setShareToClickRatio(shareRatio);
                currentInteractions.setCommentToClickRatio(commentRatio);

                currentEngagement.setTotalWatchTime(newWatchTime);

                existing.setInteractionStats(currentInteractions);
                existing.setEngagementStats(currentEngagement);

                // Save thông qua Repository (Sẽ tự Insert nếu ID chưa có, hoặc Update nếu ID đã có)
                seriesMetadataRepository.save(existing);
            }
        } catch (Exception e) {
            throw new MongoDocumentException(MongoDocumentErrorCode.ASYNC_PROCESSING_ERROR, "Total Error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("mongoDbExecutor")
    public CompletableFuture<Void> syncLast24hStats(LocalDateTime currentHour) {
        LocalDateTime start24h = currentHour.minusHours(24);
        try {
            List<SeriesLogData> results = seriesLogRepository.aggregateByHourBucketBetweenInclusiveStart(start24h, currentHour);
            if (results.isEmpty()) return CompletableFuture.completedFuture(null);

            for (SeriesLogData row : results) {
                String seriesId = row.seriesId();

                SeriesMetadata existing = seriesMetadataRepository.findById(seriesId)
                        .orElseGet(() -> SeriesMetadata.builder()
                                .id(seriesId)
                                .interactionStats(new SeriesInteractionStats())
                                .engagementStats(new SeriesEngagementStats())
                                .build());

                long clicks = safeLong(row.totalClicks());
                long likes = safeLong(row.totalLikes());
                long bookmarks = safeLong(row.totalBookmarks());
                long shares = safeLong(row.totalShares());
                long comments = safeLong(row.totalComments());

                double likeRatio = clicks > 0 ? (double) likes / clicks : 0.0;
                double bookmarkRatio = clicks > 0 ? (double) bookmarks / clicks : 0.0;
                double shareRatio = clicks > 0 ? (double) shares / clicks : 0.0;
                double commentRatio = clicks > 0 ? (double) comments / clicks : 0.0;

                SeriesInteractionStats currentInteractions = existing.getInteractionStats();
                if (currentInteractions == null) currentInteractions = new SeriesInteractionStats();

                SeriesEngagementStats currentEngagement = existing.getEngagementStats();
                if (currentEngagement == null) currentEngagement = new SeriesEngagementStats();

                currentInteractions.setClicksLast24h(clicks);
                currentInteractions.setLikesLast24h(likes);
                currentInteractions.setBookmarksLast24h(bookmarks);
                currentInteractions.setSharesLast24h(shares);
                currentInteractions.setCommentsLast24h(comments);
                currentInteractions.setLikeToClickRatioLast24h(likeRatio);
                currentInteractions.setBookmarkToClickRatioLast24h(bookmarkRatio);
                currentInteractions.setShareToClickRatioLast24h(shareRatio);
                currentInteractions.setCommentToClickRatioLast24h(commentRatio);

                currentEngagement.setWatchTimeLast24h(safeDouble(row.watchTime()));

                existing.setInteractionStats(currentInteractions);
                existing.setEngagementStats(currentEngagement);

                seriesMetadataRepository.save(existing);
            }
        } catch (Exception e) {
            throw new MongoDocumentException(MongoDocumentErrorCode.ASYNC_PROCESSING_ERROR, "24H Error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("mongoDbExecutor")
    public CompletableFuture<Void> syncLast7dStats(LocalDateTime currentHour) {
        LocalDateTime start7d = currentHour.minusDays(7);
        try {
            List<SeriesLogData> results = seriesLogRepository.aggregateByHourBucketBetweenInclusiveStart(start7d, currentHour);
            if (results.isEmpty()) return CompletableFuture.completedFuture(null);

            for (SeriesLogData row : results) {
                String seriesId = row.seriesId();

                SeriesMetadata existing = seriesMetadataRepository.findById(seriesId)
                        .orElseGet(() -> SeriesMetadata.builder()
                                .id(seriesId)
                                .interactionStats(new SeriesInteractionStats())
                                .engagementStats(new SeriesEngagementStats())
                                .build());

                long clicks = safeLong(row.totalClicks());
                long likes = safeLong(row.totalLikes());
                long bookmarks = safeLong(row.totalBookmarks());
                long shares = safeLong(row.totalShares());
                long comments = safeLong(row.totalComments());

                double likeRatio = clicks > 0 ? (double) likes / clicks : 0.0;
                double bookmarkRatio = clicks > 0 ? (double) bookmarks / clicks : 0.0;
                double shareRatio = clicks > 0 ? (double) shares / clicks : 0.0;
                double commentRatio = clicks > 0 ? (double) comments / clicks : 0.0;

                SeriesInteractionStats currentInteractions = existing.getInteractionStats();
                if (currentInteractions == null) currentInteractions = new SeriesInteractionStats();

                SeriesEngagementStats currentEngagement = existing.getEngagementStats();
                if (currentEngagement == null) currentEngagement = new SeriesEngagementStats();

                currentInteractions.setClicksLast7d(clicks);
                currentInteractions.setLikesLast7d(likes);
                currentInteractions.setBookmarksLast7d(bookmarks);
                currentInteractions.setSharesLast7d(shares);
                currentInteractions.setCommentsLast7d(comments);
                currentInteractions.setLikeToClickRatioLast7d(likeRatio);
                currentInteractions.setBookmarkToClickRatioLast7d(bookmarkRatio);
                currentInteractions.setShareToClickRatioLast7d(shareRatio);
                currentInteractions.setCommentToClickRatioLast7d(commentRatio);

                currentEngagement.setWatchTimeLast7d(safeDouble(row.watchTime()));

                existing.setInteractionStats(currentInteractions);
                existing.setEngagementStats(currentEngagement);

                seriesMetadataRepository.save(existing);
            }
        } catch (Exception e) {
            throw new MongoDocumentException(MongoDocumentErrorCode.ASYNC_PROCESSING_ERROR, "7D Error: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Thực hiện bulk update đưa các trường stats định kỳ về 0 cho danh sách ID chỉ định
     */
    @Override
    public void resetInactiveSeriesStatsInMongo(List<String> seriesIds, boolean reset24h, boolean reset7d) {
        if (seriesIds == null || seriesIds.isEmpty()) return;

        Query query = new Query(Criteria.where("id").in(seriesIds));
        Update update = new Update();

        if (reset24h) {
            update.set("interaction_stats.clicks_last_24h", 0L)
                    .set("interaction_stats.likes_last_24h", 0L)
                    .set("interaction_stats.bookmarks_last_24h", 0L)
                    .set("interaction_stats.shares_last_24h", 0L)
                    .set("interaction_stats.comments_last_24h", 0L)
                    .set("interaction_stats.like_to_click_ratio_last_24h", 0.0)
                    .set("interaction_stats.bookmark_to_click_ratio_last_24h", 0.0)
                    .set("interaction_stats.share_to_click_ratio_last_24h", 0.0)
                    .set("interaction_stats.comment_to_click_ratio_last_24h", 0.0)
                    .set("engagement_stats.watch_time_last_24h", 0.0);
        }

        if (reset7d) {
            update.set("interaction_stats.clicks_last_7d", 0L)
                    .set("interaction_stats.likes_last_7d", 0L)
                    .set("interaction_stats.bookmarks_last_7d", 0L)
                    .set("interaction_stats.shares_last_7d", 0L)
                    .set("interaction_stats.comments_last_7d", 0L)
                    .set("interaction_stats.like_to_click_ratio_last_7d", 0.0)
                    .set("interaction_stats.bookmark_to_click_ratio_last_7d", 0.0)
                    .set("interaction_stats.share_to_click_ratio_last_7d", 0.0)
                    .set("interaction_stats.comment_to_click_ratio_last_7d", 0.0)
                    .set("engagement_stats.watch_time_last_7d", 0.0);
        }

        // Thực hiện 1 câu query duy nhất cho toàn bộ danh sách ID (Bulk operation của Mongo)
        mongoTemplate.updateMulti(query, update, SeriesMetadata.class);
    }

    private long safeLong(Long val) {
        return val == null ? 0L : val;
    }

    private double safeDouble(Double val) {
        return val == null ? 0D : val;
    }
}
