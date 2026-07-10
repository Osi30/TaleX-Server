package com.talex.server.dtos.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserFeatureRequest {
    private String deviceType;
    private String os;

    // Địa lý
    private String language;
    private String timezone;

    // Profile
    private Long accountAge;
    private String registerBy;
    private String creatorTier;

    // Demographic
    private String gender;
    private Integer age;

    private List<String> onboardingMovieGenres;
    private List<String> onboardingComicGenres;
}