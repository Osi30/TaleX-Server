package com.talex.server.dtos.payout.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOSResponseDto<T> {
    private String code;
    private String desc;
    private T data;
}