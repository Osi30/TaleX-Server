package com.talex.server.dtos.responses.creator;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private Long totalViews = 0L;
    // Tính theo giờ
    private Double totalWatchTime = 0D;
    private Long likes = 0L;
    private Long bookmarks = 0L;
    private Long shares = 0L;
    private Long comments = 0L;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private TermsVersionResponseDto termsVersion;
    private Boolean isAcceptedLatestTerms = false;
}
