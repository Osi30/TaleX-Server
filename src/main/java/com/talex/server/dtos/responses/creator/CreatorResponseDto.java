package com.talex.server.dtos.responses.creator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.talex.server.entities.analytic.AnalyticData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorResponseDto {
    private String creatorId;
    private Long followerCount = 0L;
    private Long followToCount = 0L;
    private AnalyticData analyticData;
    private CreatorTierResponseDto creatorTier;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private TermsVersionResponseDto termsVersion;
    private Boolean isAcceptedLatestTerms = false;
}
