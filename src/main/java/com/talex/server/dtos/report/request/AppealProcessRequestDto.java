package com.talex.server.dtos.report.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppealProcessRequestDto {
    // true: Chấp nhận khiếu nại (Gỡ gậy), false: Bác bỏ khiếu nại
    @NotNull(message = "Kết quả xét duyệt khiếu nại không được rỗng")
    private Boolean isApproved;

    private String adminNote;
}