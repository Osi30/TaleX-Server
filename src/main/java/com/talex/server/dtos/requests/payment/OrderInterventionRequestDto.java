package com.talex.server.dtos.requests.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body cho hành động Admin can thiệp Order (hủy / hoàn tất thủ công). Lý do bắt buộc —
 * được lưu vào order_intervention_log để truy vết ai/khi nào/tại sao.
 */
@Data
public class OrderInterventionRequestDto {

    @NotBlank(message = "Lý do can thiệp là bắt buộc")
    @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
    private String reason;
}
