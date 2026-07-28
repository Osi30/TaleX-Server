package com.talex.server.dtos.mongo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(value = 10, message = "Tuổi được phép thì phải lớn hơn 10")
    @Max(value = 100)
    private Integer age;

    @NotNull
    private List<String> onboardingMovieGenres;

    @NotNull
    private List<String> onboardingComicGenres;
}