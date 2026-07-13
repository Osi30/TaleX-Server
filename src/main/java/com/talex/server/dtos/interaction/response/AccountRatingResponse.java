package com.talex.server.dtos.interaction.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRatingResponse {
    private String seriesId;
    private String seriesTitle;
    private String coverUrl;
    private String bannerUrl;
    private Double rate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
