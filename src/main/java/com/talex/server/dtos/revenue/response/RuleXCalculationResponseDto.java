package com.talex.server.dtos.revenue.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleXCalculationResponseDto {
    private Map<String, Double> episodePayouts;
    private Double gamma;                      // Hằng số Gamma vừa tìm được
    private Double targetBudget;               // Ngân sách mục tiêu = alpha * n * fee
    private Double calculatedBudget;           // Ngân sách thực tế phân bổ
    private Map<String, Double> artistPayouts; // Doanh thu chia cho từng nghệ sĩ
    private List<UserAllocationDto> userAllocations; // Chi tiết phân bổ theo từng User
}