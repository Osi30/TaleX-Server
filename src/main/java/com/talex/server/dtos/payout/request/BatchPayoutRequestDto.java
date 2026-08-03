package com.talex.server.dtos.payout.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPayoutRequestDto {

    @NotBlank(message = "Mã tham chiếu lô không được để trống")
    private String referenceId;

    private List<String> category;

    @Builder.Default
    private Boolean validateDestination = true;

    @NotEmpty(message = "Danh sách lệnh chi không được để trống")
    @Valid
    private List<PayoutItemRequestDto> payouts;
}