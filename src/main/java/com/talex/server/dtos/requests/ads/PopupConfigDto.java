package com.talex.server.dtos.requests.ads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupConfigDto {
    private List<String> allowedRoutes;
    private Long showDelayMs;
    private Integer cooldownMinutes;
}
