package com.talex.server.dtos.payout.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutItemRequestDto {

    @NotBlank(message = "Mã tham chiếu lệnh chi lẻ không được để trống")
    private String referenceId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @Positive(message = "Số tiền thanh toán phải lớn hơn 0")
    private Long amount;

    @NotBlank(message = "Mô tả thanh toán không được để trống")
    private String description;

    @NotBlank(message = "Mã ngân hàng đích (BIN) không được để trống")
    private String toBin;

    @NotBlank(message = "Số tài khoản đích không được để trống")
    private String toAccountNumber;
}