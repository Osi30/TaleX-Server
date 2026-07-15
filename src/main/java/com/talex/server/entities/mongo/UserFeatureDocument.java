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

    // 1. Thiết bị
    @Field("device_types")
    private List<String> deviceTypes = new ArrayList<>();
    private List<String> os = new ArrayList<>();

    // 2. Địa lý
    private String language;
    private String timezone;

    // 3. Profile
    @Field("account_age")
    private Long accountAge;

    @Field("register_by")
    private String registerBy;

    @Field("creator_tier")
    private String creatorTier;

    // 4. Demographic
    private String gender;
    private Integer age;

    @Field("onboarding_movie_genres")
    @Builder.Default
    private List<String> onboardingMovieGenres = new ArrayList<>();

    @Field("onboarding_comic_genres")
    @Builder.Default
    private List<String> onboardingComicGenres = new ArrayList<>();

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