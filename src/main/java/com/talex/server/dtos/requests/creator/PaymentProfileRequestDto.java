package com.talex.server.dtos.requests.creator;

import com.talex.server.enums.creator.PaymentProfileStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProfileRequestDto {
    @NotBlank(message = "Mã ngân hàng không được để trống")
    @Size(min = 2, max = 50, message = "Mã ngân hàng phải từ 2 đến 50 ký tự")
    private String bankCode;

    @NotBlank(message = "Số tài khoản không được để trống")
    @Size(min = 5, max = 50, message = "Số tài khoản phải từ 5 đến 50 ký tự")
    private String accountNumber;

    @NotBlank(message = "Tên tài khoản không được để trống")
    @Size(min = 3, max = 200, message = "Tên tài khoản phải từ 3 đến 200 ký tự")
    private String accountName;

    private Boolean isPrimary;
    private PaymentProfileStatus status;
}
