package com.talex.server.entities.subscription;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "month_year", nullable = false, length = 7)
    private String monthYear;

    @Column(name = "subscription_id", nullable = false, length = 50)
    private String subscriptionId;

    @Column(name = "viewer_id", nullable = false, length = 50)
    private String viewerId;

    @Column(name = "episode_id", nullable = false, length = 50)
    private String episodeId;

    @Column(name = "creator_id", nullable = false, length = 50)
    private String creatorId;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "is_like")
    private Boolean isLike = false;

    @Column(name = "is_comment")
    private Boolean isComment = false;

    @Column(name = "is_bookmark")
    private Boolean isBookmark = false;

    @Column(name = "is_share")
    private Boolean isShare = false;

    @Column(name = "completion_time")
    private Long completionTime = 0L;

    @Column(name = "total_time")
    private Long totalTime = 0L;

    // Trường ảo tự sinh từ DB - Khóa không cho Java can thiệp sửa đổi
    @Column(name = "weight_amount", insertable = false, updatable = false)
    private BigDecimal weightAmount;
}
