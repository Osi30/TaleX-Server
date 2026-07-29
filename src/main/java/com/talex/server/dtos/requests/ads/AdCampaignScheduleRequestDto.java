package com.talex.server.dtos.requests.ads;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdCampaignScheduleRequestDto {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
