package com.talex.server.entities.mongo;

import com.talex.server.entities.mongo.userfeatures.DeepEngagementStats;
import com.talex.server.entities.mongo.userfeatures.DynamicPreferences;
import com.talex.server.entities.mongo.userfeatures.InteractionStats;
import com.talex.server.entities.mongo.userfeatures.MonetizationStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_features")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserFeatureDocument {
    @Id
    private String accountId;

    // NHÓM ĐẶC TRƯNG TĨNH (STATIC FEATURES)

    private String language;
    private String gender;
    private Integer age;

    @Field("onboarding_movie_genres")
    @Builder.Default
    private List<String> onboardingGenres = new ArrayList<>();

    @Field("onboarding_comic_genres")
    @Builder.Default
    private List<String> onboardingTags = new ArrayList<>();

    // NHÓM ĐẶC TRƯNG ĐỘNG (DYNAMIC FEATURES)

    @Builder.Default
    private InteractionStats interactions = new InteractionStats();

    @Builder.Default
    @Field("deep_engagement")
    private DeepEngagementStats deepEngagement = new DeepEngagementStats();

    @Builder.Default
    private DynamicPreferences preferences = new DynamicPreferences();

    @Builder.Default
    private MonetizationStats monetization = new MonetizationStats();


}