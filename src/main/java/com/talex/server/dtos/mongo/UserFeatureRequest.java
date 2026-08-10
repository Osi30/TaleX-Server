package com.talex.server.dtos.mongo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private String gender;

    @NotNull
    private String age;

    @NotNull
    private List<String> onboardingGenres;

    @NotNull
    private List<String> onboardingTags;
}