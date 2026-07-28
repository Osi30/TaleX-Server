package com.talex.server.dtos.requests.ads;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdProfileSetupRequestDto {
    @NotBlank(message = "Tên công ty / thương hiệu là bắt buộc")
    private String companyName;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    private String phone;

    private String website;
}
