package com.talex.server.dtos.settlement.request;

import com.talex.server.enums.transaction.SettlementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettlementStatusRequestDto {

    @NotNull(message = "Trạng thái mới không được để trống")
    private SettlementStatus status;

    // Bắt buộc nhập khi chuyển sang UNDER_REVIEW hoặc FORFEITED
    private String note;
}