package com.talex.server.dtos.responses.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorViolationsSummaryDto {
    private String creatorId;
    private long totalCopyrightStrikes;
    private long totalCensorshipStrikes;
}
