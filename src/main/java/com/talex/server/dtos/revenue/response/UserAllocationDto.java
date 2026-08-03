package com.talex.server.dtos.revenue.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAllocationDto {
    private String userId;
    private Long totalStreams;
    private Map<String, Double> artistPayouts;
    private Map<String, Map<String, Double>> episodePayouts;
    private Double effectiveWeight;     // min(1.0, gamma * v_u)
    private Double perStreamWeight;     // min(1.0 / v_u, gamma)
    private Double allocatedAmount;     // Số tiền thực tế user này đóng góp vào pool
}