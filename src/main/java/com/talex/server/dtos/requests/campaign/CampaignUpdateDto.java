package com.talex.server.dtos.requests.campaign;

import com.talex.server.enums.engagement.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignUpdateDto {
    private CampaignStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long currentValue = 0L;
}
