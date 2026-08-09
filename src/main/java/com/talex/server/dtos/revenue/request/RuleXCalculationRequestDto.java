package com.talex.server.dtos.revenue.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleXCalculationRequestDto {

    // Giá trị gói đăng ký của mỗi user (ví dụ: 10.0 USD)
    private Double subscriptionFee;

    @NotEmpty(message = "Danh sách người dùng không được rỗng")
    private List<UserStreamRequestDto> users;
}