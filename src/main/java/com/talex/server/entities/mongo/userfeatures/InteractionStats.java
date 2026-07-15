package com.talex.server.entities.mongo.userfeatures;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InteractionStats {
    // KHỐI 1: TOÀN THỜI GIAN (LONG-TERM)
    @Field("total_clicks") @Builder.Default private Long totalClicks = 0L;
    @Field("total_likes") @Builder.Default private Long totalLikes = 0L;
    @Field("total_bookmarks") @Builder.Default private Long totalBookmarks = 0L;
    @Field("total_shares") @Builder.Default private Long totalShares = 0L;
    @Field("total_comments") @Builder.Default private Long totalComments = 0L;

    @Field("like_to_click_ratio") @Builder.Default private Double likeToClickRatio = 0.0;
    @Field("bookmark_to_click_ratio") @Builder.Default private Double bookmarkToClickRatio = 0.0;
    @Field("share_to_click_ratio") @Builder.Default private Double shareToClickRatio = 0.0;
    @Field("comment_to_click_ratio") @Builder.Default private Double commentToClickRatio = 0.0;

    // KHỐI 2: TRONG 7 NGÀY QUA (MID-TERM)
    @Field("clicks_last_7d") @Builder.Default private Long clicksLast7d = 0L;
    @Field("likes_last_7d") @Builder.Default private Long likesLast7d = 0L;
    @Field("bookmarks_last_7d") @Builder.Default private Long bookmarksLast7d = 0L;
    @Field("shares_last_7d") @Builder.Default private Long sharesLast7d = 0L;
    @Field("comments_last_7d") @Builder.Default private Long commentsLast7d = 0L;

    @Field("like_to_click_ratio_last_7d") @Builder.Default private Double likeToClickRatioLast7d = 0.0;
    @Field("bookmark_to_click_ratio_last_7d") @Builder.Default private Double bookmarkToClickRatioLast7d = 0.0;
    @Field("share_to_click_ratio_last_7d") @Builder.Default private Double shareToClickRatioLast7d = 0.0;
    @Field("comment_to_click_ratio_last_7d") @Builder.Default private Double commentToClickRatioLast7d = 0.0;

    // KHỐI 3: TRONG 24 GIỜ QUA (SHORT-TERM)
    @Field("clicks_last_24h") @Builder.Default private Long clicksLast24h = 0L;
    @Field("likes_last_24h") @Builder.Default private Long likesLast24h = 0L;
    @Field("bookmarks_last_24h") @Builder.Default private Long bookmarksLast24h = 0L;
    @Field("shares_last_24h") @Builder.Default private Long sharesLast24h = 0L;
    @Field("comments_last_24h") @Builder.Default private Long commentsLast24h = 0L;

    @Field("like_to_click_ratio_last_24h") @Builder.Default private Double likeToClickRatioLast24h = 0.0;
    @Field("bookmark_to_click_ratio_last_24h") @Builder.Default private Double bookmarkToClickRatioLast24h = 0.0;
    @Field("share_to_click_ratio_last_24h") @Builder.Default private Double shareToClickRatioLast24h = 0.0;
    @Field("comment_to_click_ratio_last_24h") @Builder.Default private Double commentToClickRatioLast24h = 0.0;
}
