package com.talex.server.dtos.report.request;

import com.talex.server.enums.report.PenaltyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketProcessRequestDto {
    // true: chấp nhận phạt, false: bác bỏ báo cáo
    @NotNull(message = "Loại hành động không được rỗng (ISSUE_PENALTY hoặc DISMISS)")
    private Boolean isApproved;

    // Bắt buộc nếu isApproved = true
    private PenaltyLevel penaltyLevel;

    @NotBlank(message = "Lý do xử lý không được để trống")
    private String reason;
}