package com.talex.server.entities.mongo;

import com.talex.server.entities.mongo.seriesfeatures.SeriesEngagementStats;
import com.talex.server.entities.mongo.seriesfeatures.SeriesInteractionStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "series_metadata")
public class SeriesMetadata {

    @Id
    private String id;
    
    @Field("content_type")
    private String contentType;
    
    private String title;
    
    private String description;
    
    private List<String> category;
    
    private List<String> tags;
    
    @Field("age_rating")
    private String ageRating;
    
    private String language;
    
    @Field("creator_tier")
    private String creatorTier;
    
    private Double rating;

    @Field("released_updated_at")
    private LocalDateTime releasedUpdatedAt;

    @Field("cover_url")
    private String coverUrl;

    @Field("banner_url")
    private String bannerUrl;

    @Field("interaction_stats")
    @Builder.Default
    private SeriesInteractionStats interactionStats = new SeriesInteractionStats();

    @Field("engagement_stats")
    @Builder.Default
    private SeriesEngagementStats engagementStats = new SeriesEngagementStats();
}
