package com.talex.server.dtos.payout.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayoutRequestProcessDto {
    @NotNull(message = "Trạng thái xử lý không được để trống")
    private String status;

    private String adminNote;
}