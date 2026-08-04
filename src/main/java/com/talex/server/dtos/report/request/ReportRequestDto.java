package com.talex.server.dtos.report.request;

import com.talex.server.enums.report.ReportReason;
import com.talex.server.enums.report.TargetType;
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
public class ReportRequestDto {
    @NotNull(message = "Target type không được để trống")
    private TargetType targetType;

    @NotBlank(message = "Target ID không được để trống")
    private String targetId;

    @NotNull(message = "Lý do báo cáo không được để trống")
    private ReportReason reason;

    private String description;
    private String proofImages;
}