package com.talex.server.dtos.mongo;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private String language;
    private String gender;
    private Integer age;

    private List<String> onboardingMovieGenres;
    private List<String> onboardingComicGenres;
}