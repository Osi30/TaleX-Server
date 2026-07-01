package com.talex.server.dtos.responses;

import com.talex.server.enums.media.CensorshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentCensorshipResponseDto {
    private String censorshipId;
    private String mediaId;
    private String primaryViolationLabel;
    private Float confidenceScore;
    private LocalDateTime checkedAt;
    private String reviewedBy;
    private String reviewerNotes;
    private CensorshipStatus status;
    private List<ViolationDetailResponseDto> violationDetails;
}
