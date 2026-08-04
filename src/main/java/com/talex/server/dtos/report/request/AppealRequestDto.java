package com.talex.server.dtos.report.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppealRequestDto {
    @NotBlank(message = "Lý do khiếu nại không được để trống")
    private String reason;

    private String proofDocuments;
}