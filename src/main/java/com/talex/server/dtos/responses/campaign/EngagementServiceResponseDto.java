package com.talex.server.dtos.responses.campaign;

import com.talex.server.enums.engagement.EngagementTarget;
import com.talex.server.enums.engagement.EngagementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementServiceResponseDto {
    private String engagementServiceId;
    private String name;
    private String description;
    private Long price;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EngagementType engagementType;
    private EngagementTarget engagementTarget;
    private Long targetValue;

}
