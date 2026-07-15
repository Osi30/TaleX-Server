package com.talex.server.entities.mongo.seriesfeatures;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesInteractionStats {
    // --- Toàn thời gian (Total) ---
    @Field("total_clicks") @Builder.Default private long totalClicks = 0L;
    @Field("total_likes") @Builder.Default private long totalLikes = 0L;
    @Field("total_bookmarks") @Builder.Default private long totalBookmarks = 0L;
    @Field("total_shares") @Builder.Default private long totalShares = 0L;
    @Field("total_comments") @Builder.Default private long totalComments = 0L;

    @Field("like_to_click_ratio") @Builder.Default private double likeToClickRatio = 0.0;
    @Field("bookmark_to_click_ratio") @Builder.Default private double bookmarkToClickRatio = 0.0;
    @Field("share_to_click_ratio") @Builder.Default private double shareToClickRatio = 0.0;
    @Field("comment_to_click_ratio") @Builder.Default private double commentToClickRatio = 0.0;

    // --- Trong 7 ngày qua ---
    @Field("clicks_last_7d") @Builder.Default private long clicksLast7d = 0L;
    @Field("likes_last_7d") @Builder.Default private long likesLast7d = 0L;
    @Field("bookmarks_last_7d") @Builder.Default private long bookmarksLast7d = 0L;
    @Field("shares_last_7d") @Builder.Default private long sharesLast7d = 0L;
    @Field("comments_last_7d") @Builder.Default private long commentsLast7d = 0L;

    @Field("like_to_click_ratio_last_7d") @Builder.Default private double likeToClickRatioLast7d = 0.0;
    @Field("bookmark_to_click_ratio_last_7d") @Builder.Default private double bookmarkToClickRatioLast7d = 0.0;
    @Field("share_to_click_ratio_last_7d") @Builder.Default private double shareToClickRatioLast7d = 0.0;
    @Field("comment_to_click_ratio_last_7d") @Builder.Default private double commentToClickRatioLast7d = 0.0;

    // --- Trong 24 giờ qua ---
    @Field("clicks_last_24h") @Builder.Default private long clicksLast24h = 0L;
    @Field("likes_last_24h") @Builder.Default private long likesLast24h = 0L;
    @Field("bookmarks_last_24h") @Builder.Default private long bookmarksLast24h = 0L;
    @Field("shares_last_24h") @Builder.Default private long sharesLast24h = 0L;
    @Field("comments_last_24h") @Builder.Default private long commentsLast24h = 0L;

    @Field("like_to_click_ratio_last_24h") @Builder.Default private double likeToClickRatioLast24h = 0.0;
    @Field("bookmark_to_click_ratio_last_24h") @Builder.Default private double bookmarkToClickRatioLast24h = 0.0;
    @Field("share_to_click_ratio_last_24h") @Builder.Default private double shareToClickRatioLast24h = 0.0;
    @Field("comment_to_click_ratio_last_24h") @Builder.Default private double commentToClickRatioLast24h = 0.0;
}
