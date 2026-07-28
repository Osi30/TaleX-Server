package com.talex.server.dtos.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStaticFeature implements Serializable {
    private String accountId;
    private String language;
    private String gender;
    private Integer age;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<String> onboardingGenres = new ArrayList<>();

    @Builder.Default
    private List<String> onboardingTags = new ArrayList<>();
}