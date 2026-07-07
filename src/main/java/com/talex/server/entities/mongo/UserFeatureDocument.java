package com.talex.server.entities.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_static_features")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserFeatureDocument {
    @Id
    private String accountId;

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
    private List<String> onboardingMovieGenres = new ArrayList<>();

    @Field("onboarding_comic_genres")
    private List<String> onboardingComicGenres = new ArrayList<>();
}